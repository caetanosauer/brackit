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
package org.brackit.xquery.operator;

import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.Tuple;
import org.brackit.xquery.xdm.Expr;
import org.brackit.xquery.xdm.Sequence;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class LetBind implements Operator {
	private final Operator in;

	private final Expr source;

	private final Expr check;

	private boolean bind = true;

	static class LetBindCursor implements Cursor {
		private final Cursor in;

		private final Expr expr;

		private final Expr check;

		public LetBindCursor(Cursor cursor, Expr expr, Expr check) {
			this.in = cursor;
			this.expr = expr;
			this.check = check;
		}

		@Override
		public void close(QueryContext ctx) {
			in.close(ctx);
		}

		@Override
		public Tuple next(QueryContext ctx) throws QueryException {
			Tuple t = in.next(ctx);

			if (t == null) {
				return null;
			}
			if ((check != null) && (check.evaluate(ctx, t) == null)) {
				return new TupleImpl(t, (Sequence) null);
			}

			Sequence sequence = expr.evaluate(ctx, t);
			return new TupleImpl(t, sequence);
		}

		@Override
		public void open(QueryContext ctx) throws QueryException {
			in.open(ctx);
		}
	}

	public LetBind(Operator in, Expr source, Expr check) {
		this.in = in;
		this.source = source;
		this.check = check;
	}

	public void bind(boolean bind) {
		this.bind = bind;
	}

	@Override
	public Cursor create(QueryContext ctx, Tuple tuple) throws QueryException {
		return (bind) ? new LetBindCursor(in.create(ctx, tuple), source, check)
				: in.create(ctx, tuple);
	}
}