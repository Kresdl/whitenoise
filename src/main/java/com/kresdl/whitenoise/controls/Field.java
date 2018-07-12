package com.kresdl.whitenoise.controls;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.function.Supplier;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import com.kresdl.whitenoise.node.Node;
import com.kresdl.whitenoise.socket.In;

@SuppressWarnings("serial")
public final class Field extends JTextField implements Supplier<Double>, Serializable, ActionListener {

    Node node;
    In in;

    public static Field crossRef(double value, In in) {
        Field c = new Field(value, in);
        in.setDefault(c);
        return c;
    }

    private Field(double value, In in) {
        this(value, in.getNode());
        this.in = in;
    }

    public Field(double value, Node node) {
        super();
        this.node = node;

        setHorizontalAlignment(SwingConstants.RIGHT);
        setColumns(4);
        setText(String.valueOf(value));
        setBorder(null);
        addActionListener(this);

        AbstractDocument d = (AbstractDocument) getDocument();
        d.setDocumentFilter(new DocumentFilter() {
            @Override
            public void replace(DocumentFilter.FilterBypass fb, int offset, int length, String text, AttributeSet attrs) {
                if (text.matches("[\\-\\d.]")) {
                    try {
                        fb.replace(offset, length, text, attrs);
                    } catch (BadLocationException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            if (!Node.isBusy()) {
                if ((in != null) && in.valid()) {
                    return;
                }
                node.fireParameterChange();
            }
        } catch (NumberFormatException t) {
        }
    }

    @Override
    public Double get() {
        return Double.valueOf(getText());
    }
}
