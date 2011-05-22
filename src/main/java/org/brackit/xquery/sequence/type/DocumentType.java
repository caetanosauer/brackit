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
package org.brackit.xquery.sequence.type;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Kind;
import org.brackit.xquery.xdm.Node;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class DocumentType extends KindTest {
	public static final DocumentType DOC = new DocumentType();

	private final ElementType elementType;

	private final SchemaElementType schemaElementType;

	public DocumentType() {
		elementType = null;
		schemaElementType = null;
	}

	public DocumentType(ElementType elementType) {
		this.elementType = elementType;
		this.schemaElementType = null;
	}

	public DocumentType(SchemaElementType schemaElementType) {
		this.elementType = null;
		this.schemaElementType = schemaElementType;
	}

	public ElementType getElementType() {
		return elementType;
	}

	public SchemaElementType getSchemaElementType() {
		return schemaElementType;
	}

	@Override
	public Kind getNodeKind() {
		return Kind.DOCUMENT;
	}

	@Override
	public boolean matches(QueryContext ctx, Node<?> node)
			throws QueryException {
		if ((elementType != null) || (schemaElementType != null)) {
			throw new QueryException(
					ErrorCode.BIT_DYN_RT_NOT_IMPLEMENTED_YET_ERROR,
					"Document type test with element or schema element test support not implemented yet");
		}
		return (node.getKind() == Kind.DOCUMENT);
	}

	@Override
	public boolean matches(QueryContext ctx, Item item) throws QueryException {
		if ((elementType != null) || (schemaElementType != null)) {
			throw new QueryException(
					ErrorCode.BIT_DYN_RT_NOT_IMPLEMENTED_YET_ERROR,
					"Document type test with element or schema element test support not implemented yet");
		}
		return ((item instanceof Node<?>) && (((Node<?>) item).getKind() == Kind.DOCUMENT));
	}

	public String toString() {
		return (elementType != null) ? String.format("document-node(\"%s\")",
				elementType) : (schemaElementType != null) ? String.format(
				"document-node(\"%s\")", schemaElementType) : "document-node()";
	}
}