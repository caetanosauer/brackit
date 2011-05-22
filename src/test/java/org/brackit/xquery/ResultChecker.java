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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.Tuple;
import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.atomic.Int32;
import org.brackit.xquery.operator.Cursor;
import org.brackit.xquery.operator.TupleImpl;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Node;
import org.brackit.xquery.xdm.Sequence;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class ResultChecker {
	public static void dCheckS(QueryContext ctx, Cursor op,
			Sequence... expected) throws QueryException {
		// double check result to verify that plan can be evaluated repeatedly
		checkS(ctx, op, expected);
		checkS(ctx, op, expected);
	}

	public static void checkS(QueryContext ctx, Cursor op, Sequence... expected)
			throws QueryException {
		Tuple[] e = new Tuple[expected.length];
		for (int i = 0; i < expected.length; i++) {
			e[i] = new TupleImpl(expected[i]);
		}
		checkT(ctx, op, e);
	}

	public static void dCheckT(QueryContext ctx, Cursor op, Tuple... expected)
			throws QueryException {
		// double check result to verify that plan can be evaluated repeatedly
		checkT(ctx, op, expected);
		checkT(ctx, op, expected);
	}

	public static void checkT(QueryContext ctx, Cursor op, Tuple... expected)
			throws QueryException {
		Tuple next;
		op.open(ctx);
		for (int i = 0; i < expected.length; i++) {
			assertNotNull("Result is empty", next = op.next(ctx));
			checkTuple(ctx, expected[i], next);
		}
		assertNull("No more results delivered", op.next(ctx));
		op.close(ctx);
	}

	public static void checkTuple(QueryContext ctx, Tuple expected, Tuple result)
			throws QueryException {
		int eSize = expected.getSize();
		int rSize = result.getSize();
		assertEquals("Result tuple has same size", eSize, rSize);
		for (int i = 0; i < eSize; i++) {
			Sequence eSequence = expected.get(i);
			Sequence rSequence = result.get(i);
			check(ctx, eSequence, rSequence);
		}
	}

	public static void dCheck(QueryContext ctx, Sequence expected,
			Sequence result) throws QueryException {
		// double check result to verify that result can be evaluated repeatedly
		check(ctx, expected, result);
		check(ctx, expected, result);
	}

	public static void check(QueryContext ctx, Sequence expected,
			Sequence result) throws QueryException {
		if (expected == null) {
			if (result != null) // verify that result sequence has no results
			{
				Iter s = result.iterate();
				try {
					assertNull("Result sequence is empty", s.next());
				} finally {
					s.close();
				}
				assertFalse("Result has boolean value of empty sequence",
						result.booleanValue(ctx));
				assertTrue("Result has size of empty sequence", Int32.ZERO
						.cmp(result.size(ctx)) == 0);
			}
		} else {
			assertNotNull("Result sequence is not empty", result);
			Iter es = expected.iterate();
			try {
				Iter rs = result.iterate();
				try {
					Item eItem;
					Item rItem;
					while ((eItem = es.next()) != null) {
						assertNotNull("Result sequence has more results",
								rItem = rs.next());
						try {
							assertEquals("Result item has same type", eItem
									.type(), rItem.type());

							if (eItem instanceof Node<?>) {
								assertTrue("Result item is node",
										rItem instanceof Node<?>);
								assertTrue("Result node is equal to expected",
										((Node<?>) eItem)
												.isSelfOf((Node<?>) rItem));
							} else {
								assertTrue("Result item is atomic",
										rItem instanceof Atomic);
								assertTrue(
										"Result atomic is equal to expected",
										((Atomic) eItem).eq((Atomic) rItem));
							}
						} catch (AssertionError e) {
							System.err.println(String.format(
									"Expected: '%s'\t Result: '%s'", eItem,
									rItem));
							throw e;
						} catch (QueryException e) {
							if (e.getCode() == ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE) {
								fail(String
										.format(
												"Wrong item (type) in result. Expected: '%s'\t Result: '%s'",
												eItem, rItem));
							}
							throw e;
						}
					}
					assertNull(
							"Result sequence has not more results than expected",
							rs.next());
				} finally {
					rs.close();
				}
			} finally {
				es.close();
			}

			try {
				boolean expectedBooleanValue = expected.booleanValue(ctx);
				boolean resultBooleanValue = false;

				try {
					resultBooleanValue = result.booleanValue(ctx);
				} catch (QueryException e) {
					fail("Result does not have defined boolean value");
				}

				assertEquals("Result has expected boolean value",
						expectedBooleanValue, resultBooleanValue);
			} catch (QueryException e) {
				assertEquals("Correct error code",
						ErrorCode.ERR_INVALID_ARGUMENT_TYPE, e.getCode());

				try {
					result.booleanValue(ctx);
					fail("Result has defined boolean value");
				} catch (QueryException e1) {
					assertEquals("Correct error code",
							ErrorCode.ERR_INVALID_ARGUMENT_TYPE, e1.getCode());
				}
			}
		}
	}
}