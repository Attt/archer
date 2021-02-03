package com.github.attt.archer.cache.preset;

import com.caucho.hessian.io.HessianInput;
import com.caucho.hessian.io.HessianOutput;
import com.github.attt.archer.cache.ValueSerializer;

import java.io.*;

/**
 * @author atpexgo.wu
 * @since 1.0
 */
public class HessianObjectSerializer<T> implements ValueSerializer<T> {


    @Override
    public T deserialize(byte[] serialized) {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(serialized);
             HessianInputAutoCloseable hessianInput = new HessianInputAutoCloseable(byteArrayInputStream);) {
            return (T) hessianInput.readObject();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] serialize(T raw) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             HessianOutputAutoCloseable hessianOutput = new HessianOutputAutoCloseable(byteArrayOutputStream)) {
            hessianOutput.writeObject(raw);
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    static class HessianOutputAutoCloseable extends HessianOutput implements AutoCloseable {
        public HessianOutputAutoCloseable(OutputStream os) {
            super(os);
        }
    }

    static class HessianInputAutoCloseable extends HessianInput implements AutoCloseable {

        public HessianInputAutoCloseable(InputStream is) {
            super(is);
        }

    }
}
