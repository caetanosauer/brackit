/*
 * [New BSD License]
 * Copyright (c) 2011, Brackit Project Team <info@brackit.org>  
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.brackit.xquery;

import java.io.PrintStream;
import java.io.PrintWriter;

import org.apache.log4j.Logger;
import org.brackit.xquery.compiler.AST;
import org.brackit.xquery.compiler.BottomUpCompiler;
import org.brackit.xquery.compiler.optimizer.walker.DoSNStepMerger;
import org.brackit.xquery.compiler.optimizer.walker.JoinRewriter2;
import org.brackit.xquery.compiler.optimizer.walker.JoinSortElimination;
import org.brackit.xquery.compiler.optimizer.walker.LeftJoinGroupEmission;
import org.brackit.xquery.compiler.optimizer.walker.LetBindLift;
import org.brackit.xquery.compiler.optimizer.walker.LetVariableRefPullup;
import org.brackit.xquery.compiler.optimizer.walker.UnnestRewriter;
import org.brackit.xquery.compiler.parser.ANTLRParser;
import org.brackit.xquery.compiler.parser.DotUtil;
import org.brackit.xquery.module.MainModule;
import org.brackit.xquery.node.SubtreePrinter;
import org.brackit.xquery.operator.TupleImpl;
import org.brackit.xquery.util.Cfg;
import org.brackit.xquery.xdm.Expr;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Kind;
import org.brackit.xquery.xdm.Node;
import org.brackit.xquery.xdm.Sequence;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class XQuery {
	private static final Logger log = Logger.getLogger(XQuery.class);

	public static final String VARIABLE_PULLUP_CFG = "org.brackit.xquery.variablePullup";

	public static final String JOIN_DETECTION_CFG = "org.brackit.xquery.joinDetection";

	public static final String UNNEST_CFG = "org.brackit.xquery.unnest";

	public static final String DEBUG_CFG = "org.brackit.xquery.debug";

	public static final String DEBUG_DIR_CFG = "org.brackit.xquery.debugDir";

	public static final boolean DEBUG = Cfg.asBool(DEBUG_CFG, false);

	public static final boolean UNNEST = Cfg.asBool(UNNEST_CFG, false);

	public static final boolean VARIABLE_PULLUP = Cfg.asBool(
			VARIABLE_PULLUP_CFG, false);

	public static final boolean JOIN_DETECTION = Cfg.asBool(JOIN_DETECTION_CFG,
			false);

	public static final String DEBUG_DIR = Cfg.asString(DEBUG_DIR_CFG, "debug");

	private final String query;

	private MainModule module;

	private long parsingTime;

	private long normalizeTiming;

	private long compileTiming;

	private boolean prettyPrint;

	public XQuery(MainModule module) {
		this.query = null;
		this.module = module;
	}

	public XQuery(String query) throws QueryException {
		this.query = query;
		compile(UNNEST);
	}

	public MainModule getMainModule() {
		return module;
	}

	private XQuery compile(boolean unnest) throws QueryException {
		if (DEBUG) {
			System.out.println(String.format("Compiling query:\n%s", query));
		}

		AST ast = new ANTLRParser().parse(query);
		ast = standardRewrite(ast);
		ast = (unnest) ? unnestRewrite(ast) : ast;

		module = translate(ast);

		if (DEBUG) {
			DotUtil.drawDotToFile(ast.dot(), DEBUG_DIR, "xquery");
		}
		return this;
	}

	private AST standardRewrite(AST ast) {
		new DoSNStepMerger().walk(ast);

		if (VARIABLE_PULLUP) {
			new LetVariableRefPullup().walk(ast);
		}

		if (DEBUG) {
			DotUtil.drawDotToFile(ast.dot(), DEBUG_DIR, "standardrewrite");
		}

		return ast;
	}

	private AST unnestRewrite(AST ast) throws QueryException {
		new UnnestRewriter().walk(ast);

		if (DEBUG) {
			DotUtil.drawDotToFile(ast.dot(), DEBUG_DIR, "unnestrewrite");
		}

		//new JoinRewriter2().walk(ast);

		if (DEBUG) {
			DotUtil.drawDotToFile(ast.dot(), DEBUG_DIR, "joinrewrite");
		}

		new LetBindLift().walk(ast);

		if (DEBUG) {
			DotUtil.drawDotToFile(ast.dot(), DEBUG_DIR, "letbindliftrewrite");
		}

		new JoinSortElimination().walk(ast);

		if (DEBUG) {
			DotUtil.drawDotToFile(ast.dot(), DEBUG_DIR,
					"joinsorteliminationrewrite");
		}

		new LeftJoinGroupEmission().walk(ast);

		if (DEBUG) {
			DotUtil.drawDotToFile(ast.dot(), DEBUG_DIR,
					"joingroupemissionrewrite");
		}

		// new ReturnExprDecouple().walk(ast);
		//		
		// if (DEBUG)
		// {
		// DotUtil.drawDotToFile(ast.dot(), DEBUG_DIR,
		// "ReturnExprDecoubleRewrite");
		// }

		return ast;
	}

	private MainModule translate(AST ast) throws QueryException {
		long start = System.currentTimeMillis();
		org.brackit.xquery.compiler.Compiler compiler = new BottomUpCompiler();
		MainModule module = (MainModule) compiler.xquery(ast);
		long end = System.currentTimeMillis();
		compileTiming = end - start;
		return module;
	}

	public Sequence execute(QueryContext ctx) throws QueryException {
		Expr rootExpr = module.getRootExpr();
		Sequence result = rootExpr.evaluate(ctx, new TupleImpl());

		if (rootExpr.isUpdating()) {
			// iterate possibly lazy result sequence to "pull-in" all pending
			// updates
			if ((result != null) && (!(result instanceof Item))) {
				Iter it = result.iterate();
				try {
					while (it.next() != null)
						;
				} finally {
					it.close();
				}
			}
			ctx.applyUpdates();
		}

		return result;
	}

	public void serialize(QueryContext ctx, PrintStream out)
			throws QueryException {
		serialize(ctx, new PrintWriter(out));
	}

	public void serialize(QueryContext ctx, PrintWriter out)
			throws QueryException {
		Sequence result = execute(ctx);

		if (result == null) {
			return;
		}

		boolean first = true;
		SubtreePrinter printer = new SubtreePrinter(out);
		printer.setPrettyPrint(prettyPrint);
		printer.setAutoFlush(false);
		Item item;
		Iter it = result.iterate();
		try {
			while ((item = it.next()) != null) {
				if (item instanceof Node<?>) {
					Node<?> node = (Node<?>) item;
					Kind kind = node.getKind();

					if ((kind == Kind.ATTRIBUTE) || (kind == Kind.NAMESPACE)) {
						throw new QueryException(
								ErrorCode.ERR_SERIALIZE_ATTRIBUTE_OR_NAMESPACE_NODE);
					}
					if (kind == Kind.DOCUMENT) {
						node = node.getFirstChild();
						while (node.getKind() != Kind.ELEMENT) {
							node = node.getNextSibling();
						}
					}

					printer.print(node);
					first = true;
				} else {
					if (!first) {
						out.write(" ");
					}
					out.write(item.toString());
					first = false;
				}
			}
		} finally {
			printer.flush();
			out.flush();
			it.close();
		}
	}

	public boolean isPrettyPrint() {
		return prettyPrint;
	}

	public void setPrettyPrint(boolean prettyPrint) {
		this.prettyPrint = prettyPrint;
	}

	public void printTimings() {
		System.out.println("Parse:\t" + parsingTime);
		System.out.println("Normalize:\t" + normalizeTiming);
		System.out.println("Compile:\t" + compileTiming);
	}
}