/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.openjdk.asmtools.jasm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.openjdk.asmtools.jasm.RuntimeConstants.*;

/**
 * The record attribute (JEP 359 since class file 58.65535)
 */
public class RecordData extends AttrData {
    private List<ComponentData> components = new ArrayList<>();
    private ComponentData currentComp = null;

    /* Record Data Fields */
    public RecordData(ClassData cls) {
        super(cls, Tables.AttrTag.ATT_Record.parsekey());
    }

    public void addComponent(ConstantPool.ConstCell nameCell, ConstantPool.ConstCell descCell) {
        // Define a field if absent
        FieldData fd = getClassData().addFieldIfAbsent(ACC_MANDATED & ACC_PRIVATE & ACC_FINAL, nameCell, descCell);
        currentComp = new ComponentData(fd);
        components.add(currentComp);
    }

    public void setComponentSignature(ConstantPool.ConstCell signature) {
        currentComp.setSignature(signature);
    }

    public void setAnnotations(ArrayList<AnnotationData> annotations) {
        currentComp.addAnnotations(annotations);
    }

    @Override
    public void write(CheckedDataOutputStream out) throws IOException {
        super.write(out);
        out.writeShort(components.size());
        for (ComponentData cd : components) {
            cd.write(out);
        }
    }

    @Override
    public int attrLength() {
        int compsLength = components.stream().mapToInt(c -> c.getLength()).sum();
        return 2 + compsLength;
    }

    class ComponentData extends MemberData {
        private FieldData field;
        private AttrData signature;

        public ComponentData(FieldData field) {
            super(getClassData());
            this.field = field;
        }

        public void setSignature(ConstantPool.ConstCell value_cpx) {
            signature = new CPXAttr(cls, Tables.AttrTag.ATT_Signature.parsekey(), value_cpx);
        }

        @Override
        protected DataVector getAttrVector() {
            return getDataVector(signature);
        }

        public void write(CheckedDataOutputStream out) throws IOException, Parser.CompilerError {
            out.writeShort(field.getNameDesc().left.arg);
            out.writeShort(field.getNameDesc().right.arg);
            DataVector attrs = getAttrVector();
            attrs.write(out);
        }

        public int getLength() {
            return 4 + getAttrVector().getLength();
        }
    }
}