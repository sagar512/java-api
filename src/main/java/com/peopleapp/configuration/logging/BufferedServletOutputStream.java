package com.peopleapp.configuration.logging;

import org.apache.commons.io.output.TeeOutputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import java.io.IOException;
import java.io.OutputStream;

public class BufferedServletOutputStream extends ServletOutputStream {

    private final TeeOutputStream targetStream;

    public BufferedServletOutputStream(OutputStream one, OutputStream two) {
        targetStream = new TeeOutputStream(one, two);
    }

    @Override
    public void write(int arg0) throws IOException {
        this.targetStream.write(arg0);
    }

    @Override
    public void flush() throws IOException {
        super.flush();
        this.targetStream.flush();
    }

    @Override
    public void close() throws IOException {
        super.close();
        this.targetStream.close();
    }

    @Override
    public boolean isReady() {
        return false;
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {
        // abstract method overridden from parent class
    }
}
