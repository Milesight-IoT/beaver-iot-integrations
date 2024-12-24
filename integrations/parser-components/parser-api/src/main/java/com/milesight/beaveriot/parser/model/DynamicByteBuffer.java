package com.milesight.beaveriot.parser.model;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * 动态字节缓冲区
 */
public class DynamicByteBuffer {

    // 使用 List 来存储多个 ByteBuffer
    private final List<ByteBuffer> buffers;

    public DynamicByteBuffer() {
        buffers = new ArrayList<>();
    }

    public void append(byte[] data) {
        int remaining = data.length;
        int offset = 0;

        while (remaining > 0) {
            ByteBuffer currentBuffer = getCurrentBuffer();
            int availableSpace = currentBuffer.remaining();

            if (availableSpace >= remaining) {
                currentBuffer.put(data, offset, remaining);
                break;
            } else {
                currentBuffer.put(data, offset, availableSpace);
                offset += availableSpace;
                remaining -= availableSpace;
            }
        }
    }

    public byte[] toArray() {
        ByteBuffer consolidatedBuffer = consolidateBuffers();

        int size = consolidatedBuffer.position();
        byte[] result = new byte[size];

        consolidatedBuffer.rewind();
        consolidatedBuffer.get(result);

        return result;
    }

    private ByteBuffer getCurrentBuffer() {
        // 没有可用的缓冲区，或者当前缓冲区已满
        if (buffers.isEmpty() || !buffers.get(buffers.size() - 1).hasRemaining()) {
            // 自定义缓冲区大小
            int bufferSize = Math.max(4096, buffers.size() * 2);
            ByteBuffer newBuffer = ByteBuffer.allocate(bufferSize);
            buffers.add(newBuffer);
        }

        return buffers.get(buffers.size() - 1);
    }

    private ByteBuffer consolidateBuffers() {
        int totalSize = getTotalSize();
        ByteBuffer consolidatedBuffer = ByteBuffer.allocate(totalSize);

        for (ByteBuffer buffer : buffers) {
            buffer.flip();
            consolidatedBuffer.put(buffer);
        }
        return consolidatedBuffer;
    }

    public int getTotalSize() {
        int totalSize = 0;

        for (ByteBuffer buffer : buffers) {
            totalSize += buffer.position();
        }

        return totalSize;
    }

    public void clear() {
        buffers.clear();
    }
}
