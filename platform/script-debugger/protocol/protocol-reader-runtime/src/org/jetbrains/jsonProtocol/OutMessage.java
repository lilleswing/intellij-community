package org.jetbrains.jsonProtocol;

import com.google.gson.stream.JsonWriter;
import com.intellij.openapi.vfs.CharsetToolkit;
import gnu.trove.TIntArrayList;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntProcedure;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtf8Writer;
import io.netty.buffer.ByteBufUtilEx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.io.JsonUtil;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public abstract class OutMessage {
  // todo don't want to risk - are we really can release it properly? heap buffer for now
  private final ByteBuf buffer = ByteBufAllocator.DEFAULT.heapBuffer();
  public final JsonWriter writer = new JsonWriter(new ByteBufUtf8Writer(buffer));

  private boolean finalized;

  protected OutMessage() {
    try {
      writer.beginObject();
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected void beginArguments() throws IOException {
  }

  protected final void writeEnum(String name, Enum<?> value) {
    try {
      beginArguments();
      writer.name(name).value(value.toString());
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected final void writeMap(String name, Map<String, String> value) {
    try {
      beginArguments();
      writer.name(name);
      writer.beginObject();
      for (Map.Entry<String, String> entry : value.entrySet()) {
        writer.name(entry.getKey()).value(entry.getValue());
      }
      writer.endObject();
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public final void writeInt(String name, int value) {
    try {
      beginArguments();
      writer.name(name).value(value);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected final void writeLongArray(String name, long[] value) {
    try {
      beginArguments();
      writer.name(name);
      writer.beginArray();
      for (long v : value) {
        writer.value(v);
      }
      writer.endArray();
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected final void writeDoubleArray(String name, double[] value) {
    try {
      beginArguments();
      writer.name(name);
      writer.beginArray();
      for (double v : value) {
        writer.value(v);
      }
      writer.endArray();
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected final void writeIntArray(@NotNull String name, @NotNull int[] value) {
    try {
      beginArguments();
      writer.name(name);
      writer.beginArray();
      for (int v : value) {
        writer.value(v);
      }
      writer.endArray();
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected final void writeIntSet(@NotNull String name, @NotNull TIntHashSet value) {
    try {
      beginArguments();
      writer.name(name);
      writer.beginArray();
      value.forEach(new TIntProcedure() {
        @Override
        public boolean execute(int value) {
          try {
            writer.value(value);
          }
          catch (IOException e) {
            throw new RuntimeException(e);
          }
          return true;
        }
      });
      writer.endArray();
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }

  }

  protected final void writeIntList(@NotNull String name, @NotNull TIntArrayList value) {
    try {
      beginArguments();
      writer.name(name);
      writer.beginArray();
      for (int i = 0; i < value.size(); i++) {
        writer.value(value.getQuick(i));
      }
      writer.endArray();
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected final void writeSingletonIntArray(@NotNull String name, int value) {
    try {
      beginArguments();
      writer.name(name);
      writer.beginArray();
      writer.value(value);
      writer.endArray();
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected final <E extends OutMessage> void writeList(String name, List<E> value) {
    if (value == null || value.isEmpty()) {
      return;
    }

    try {
      beginArguments();
      writer.name(name);
      writer.beginArray();
      boolean isNotFirst = false;
      for (OutMessage item : value) {
        if (isNotFirst) {
          buffer.writeByte(',').writeByte(' ');
        }
        else {
          isNotFirst = true;
        }

        if (!item.finalized) {
          item.finalized = true;
          try {
            item.writer.endObject();
          }
          catch (IllegalStateException e) {
            if ("Nesting problem.".equals(e.getMessage())) {
              throw new RuntimeException(item.buffer.toString(CharsetToolkit.UTF8_CHARSET) + "\nparent:\n" + buffer.toString(CharsetToolkit.UTF8_CHARSET), e);
            }
            else {
              throw e;
            }
          }
        }

        buffer.writeBytes(item.buffer);
      }
      writer.endArray();
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected final void writeStringList(@NotNull String name, @NotNull Collection<String> value) {
    try {
      beginArguments();
      JsonWriters.writeStringList(writer, name, value);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static void prepareWriteRaw(@NotNull OutMessage message, @NotNull String name) throws IOException {
    message.writer.name(name).nullValue();
    ByteBuf itemBuffer = message.buffer;
    itemBuffer.writerIndex(itemBuffer.writerIndex() - "null".length());
  }

  public static void doWriteRaw(@NotNull OutMessage message, @NotNull String rawValue) {
    ByteBufUtilEx.writeUtf8(message.buffer, rawValue);
  }

  protected final void writeMessage(@NotNull String name, @NotNull OutMessage value) {
    try {
      beginArguments();
      prepareWriteRaw(this, name);

      if (!value.finalized) {
        value.close();
      }
      buffer.writeBytes(value.buffer);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void close() throws IOException {
    assert !finalized;
    finalized = true;
    writer.endObject();
    writer.close();
  }

  protected final void writeLong(String name, long value) {
    try {
      beginArguments();
      writer.name(name).value(value);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected final void writeDouble(String name, double value) {
    try {
      beginArguments();
      writer.name(name).value(value);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected final void writeBoolean(String name, boolean value) {
    try {
      beginArguments();
      writer.name(name).value(value);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected final void writeString(@NotNull String name, @Nullable String value) {
    if (value != null) {
      writeNullableString(name, value);
    }
  }

  protected final void writeString(@NotNull String name, CharSequence value) {
    if (value != null) {
      try {
        prepareWriteRaw(this, name);
        JsonUtil.escape(value, buffer);
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  protected final void writeNullableString(@NotNull String name, @Nullable String value) {
    try {
      beginArguments();
      writer.name(name).value(value);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @NotNull
  @SuppressWarnings("UnusedDeclaration")
  public final ByteBuf getBuffer() {
    return buffer;
  }
}