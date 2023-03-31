package com.peopleapp.configuration.logging;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.*;

final class BufferedRequestWrapper extends
        HttpServletRequestWrapper {


    private ByteArrayOutputStream baos = null;
    private byte[] buffer = null;

    public BufferedRequestWrapper(HttpServletRequest req)
            throws IOException {
        super(req);
        // Read InputStream and store its content in a buffer.
        InputStream is = req.getInputStream();
        this.baos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int read;
        while ((read = is.read(buf)) > 0) {
            this.baos.write(buf, 0, read);
        }
        this.buffer = this.baos.toByteArray();
    }

    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream bais = new ByteArrayInputStream(this.buffer);

        return new BufferedServletInputStream(bais);
    }

    String getRequestBody() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                this.getInputStream()));
        String line = null;
        StringBuilder inputBuffer = new StringBuilder();
        do {
            line = reader.readLine();
            if (null != line) {
                inputBuffer.append(line.trim());
            }
        } while (line != null);
        reader.close();
        return inputBuffer.toString().trim();
    }

}
