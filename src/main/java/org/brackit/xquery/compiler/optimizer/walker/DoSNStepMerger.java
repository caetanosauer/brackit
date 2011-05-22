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
package org.brackit.xquery.compiler.optimizer.walker;

import org.brackit.xquery.compiler.AST;
import org.brackit.xquery.compiler.parser.XQueryParser;

/**
 * Merge descendant-or-self::node/child::X to descendant::X
 * 
 * @author Sebastian Baechle
 * 
 */
public class DoSNStepMerger extends Walker {
	@Override
	protected AST visit(AST node) {
		if (node.getType() != XQueryParser.PathExpr) {
			return node;
		}

		for (int i = 0; i < node.getChildCount(); i++) {
			AST child = node.getChild(i);
			if ((isDescendantOrSelfNodeStepWithoutPredicate(child))
					&& (i + 1 < node.getChildCount())) {
				AST nextStep = node.getChild(i + 1);

				if (isChildStepWithoutPredicate(nextStep)) {
					AST newStep = new AST(XQueryParser.StepExpr, "StepExpr");
					AST axis = new AST(XQueryParser.AxisSpec, "AxisSpec");
					axis
							.addChild(new AST(XQueryParser.DESCENDANT,
									"DESCENDANT"));
					newStep.addChild(axis);
					newStep.addChild(nextStep.getChild(1).copyTree());
					node.replaceChild(i, newStep);
					node.deleteChild(i + 1);
					child = newStep;

					snapshot();
				}
			}
		}

		return node;
	}

	private boolean isChildStepWithoutPredicate(AST child) {
		return ((child.getType() == XQueryParser.StepExpr)
				&& (child.getChild(0).getType() == XQueryParser.AxisSpec)
				&& (child.getChild(0).getChild(0).getType() == XQueryParser.CHILD) && (child
				.getChildCount() == 2)); // no predicate
	}

	private boolean isDescendantOrSelfNodeStepWithoutPredicate(AST child) {
		return ((child.getType() == XQueryParser.StepExpr)
				&& (child.getChild(0).getType() == XQueryParser.AxisSpec)
				&& (child.getChild(0).getChild(0).getType() == XQueryParser.DESCENDANT_OR_SELF)
				&& (child.getChild(1).getType() == XQueryParser.KindTestAnyKind) && (child
				.getChildCount() == 2)); // no predicate
	}
}