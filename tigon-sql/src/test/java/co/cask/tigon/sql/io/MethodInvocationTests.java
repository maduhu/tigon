/*
 * Copyright © 2014 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package co.cask.tigon.sql.io;

import co.cask.tigon.internal.io.Schema;
import co.cask.tigon.internal.io.UnsupportedTypeException;
import co.cask.tigon.internal.lang.MethodVisitor;
import co.cask.tigon.internal.lang.Reflections;
import co.cask.tigon.sql.flowlet.AbstractInputFlowlet;
import co.cask.tigon.sql.flowlet.GDATFieldType;
import co.cask.tigon.sql.flowlet.StreamSchema;
import co.cask.tigon.sql.flowlet.annotation.QueryOutput;
import com.google.common.collect.Maps;
import com.google.common.primitives.Longs;
import com.google.common.reflect.TypeToken;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Map;

/**
 * MethodsDriverTest
 * This test class tests all components associated with flowlet method invocation
 */
public class MethodInvocationTests {
  private static AbstractInputFlowlet flowlet;
  private static MethodsDriver driver;
  private static Schema schema;
  private static byte[] bytes;

  @BeforeClass
  public static void setup() throws Exception {
    flowlet = new GenericFlowletTestClass();
    StreamSchema streamSchema = new StreamSchema.Builder()
      .addField("timestamp", GDATFieldType.INT)
      .addField("iStream", GDATFieldType.INT)
      .addField("stringVar", GDATFieldType.STRING)
      .addField("longVar", GDATFieldType.LONG)
      .addField("doubleVar", GDATFieldType.DOUBLE)
      .addField("boolVar", GDATFieldType.BOOL)
      .build();
    Map<String, StreamSchema> schemaMap = Maps.newHashMap();
    schemaMap.put("sumOut", streamSchema);
    driver = new MethodsDriver(flowlet, schemaMap);
    schema = driver.getSchema(streamSchema);
    GDATEncoder encoder = new GDATEncoder();
    encoder.writeInt(23);
    encoder.writeInt(456789);
    encoder.writeString("I am your POJO!");
    encoder.writeLong(Longs.MAX_POWER_OF_TWO);
    encoder.writeDouble(13.123);
    encoder.writeBool(false);
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    encoder.writeTo(bos);
    bytes = bos.toByteArray();
  }

  /**
   * End-to-End Test
   * Tests the invocation of all Methods in a sample flowlet
   * [{@link co.cask.tigon.sql.io.GenericFlowletTestClass}]
   *
   * Components tested :
   * {@link co.cask.tigon.sql.io.MethodsDriver}
   * {@link co.cask.tigon.sql.io.MethodInvoker}
   * {@link co.cask.tigon.sql.io.POJOCreator}
   */
  @Test
  public void testMethodDriver() throws InvocationTargetException, IllegalAccessException {
    driver.invokeMethods("sumOut", new GDATDecoder(ByteBuffer.wrap(bytes)));
  }

  /**
   * Tests expected POJOCreator
   */
  @Test
  public void testPOJOCreator() throws IOException, UnsupportedTypeException {
    POJOCreator pojoCreator = new POJOCreator(Output1.class, schema);
    Output1 obj = (Output1) pojoCreator.decode(new GDATDecoder(ByteBuffer.wrap(bytes)));
    Assert.assertEquals("\tTimeStamp: 23\tiStream: 456789\tString: I am your POJO!", obj.toString());
    Assert.assertEquals(Output1.class, obj.getClass());
  }

  /**
   * NOTE: This test generates error logs
   * Tests the POJOCreator when a bad data record is provided
   */
  @Test(expected = IOException.class)
  public void testPOJOCreatorErrorRecord() throws IOException, UnsupportedTypeException {
    POJOCreator pojoCreator = new POJOCreator(Output1.class, schema);
    GDATEncoder encoder = new GDATEncoder();
    encoder.writeInt(23);
    encoder.writeString("I am your POJO!");
    encoder.writeDouble(13.123);
    encoder.writeBool(false);
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    encoder.writeTo(bos);
    pojoCreator.decode(new GDATDecoder(ByteBuffer.wrap(bos.toByteArray())));
  }

  /**
   * When an incorrect variable name is used. The assigned value will be null.
   * @throws IOException
   * @throws UnsupportedTypeException
   */
  @Test
  public void testPOJOCreatorBadVarName() throws IOException, UnsupportedTypeException {
    POJOCreator pojoCreator = new POJOCreator(Output3.class, schema);
    Output3 obj = (Output3) pojoCreator.decode(new GDATDecoder(ByteBuffer.wrap(bytes)));
    Assert.assertEquals(Output3.class, obj.getClass());
    Assert.assertEquals(null, obj.badDoubleVar);
  }

  /**
   * NOTE: This test generates error logs
   * @throws IOException : When the data types are incompatible
   * @throws UnsupportedTypeException
   */
  @Test (expected = IOException.class)
  public void testPOJOCreatorWrongDataType() throws IOException, UnsupportedTypeException {
    POJOCreator pojoCreator = new POJOCreator(Output4.class, schema);
    pojoCreator.decode(new GDATDecoder(ByteBuffer.wrap(bytes)));
  }

  /**
   * Tests the expected use case of method invoker
   */
  @Test
  public void testMethodInvoker() {
    Reflections.visit(flowlet, TypeToken.of(flowlet.getClass()),
                      new MethodVisitor() {
                        @Override
                        public void visit(Object o, TypeToken<?> inspectType, TypeToken<?> declareType, Method method)
                          throws Exception {
                          QueryOutput annotation = method.getAnnotation(QueryOutput.class);
                          if (annotation == null) {
                            return;
                          }
                          MethodInvoker methodInvoker = new MethodInvoker(o, method, inspectType, schema);
                          methodInvoker.invoke(new GDATDecoder(ByteBuffer.wrap(bytes)));
                        }
                      }
    );
  }
}
