/*
 * Copyright 2016 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.navercorp.pinpoint.profiler.instrument;

import com.navercorp.pinpoint.bootstrap.instrument.InstrumentClass;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentContext;
import com.navercorp.pinpoint.bootstrap.instrument.NotFoundInstrumentException;
import com.navercorp.pinpoint.common.util.Assert;
import com.navercorp.pinpoint.profiler.util.JavaAssistUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.Instrumentation;
import java.util.jar.JarFile;

/**
 * @author jaehong.kim
 */
public class ASMEngine implements InstrumentEngine {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final boolean isInfo = logger.isInfoEnabled();

    private final Instrumentation instrumentation;
    private final EngineComponent engineComponent;


    public ASMEngine(Instrumentation instrumentation, EngineComponent engineComponent) {
        this.instrumentation = Assert.requireNonNull(instrumentation, "instrumentation must not be null");
        this.engineComponent = Assert.requireNonNull(engineComponent, "engineComponent must not be null");
    }

    @Override
    public InstrumentClass getClass(InstrumentContext instrumentContext, ClassLoader classLoader, String className, byte[] classFileBuffer) throws NotFoundInstrumentException {
        if (className == null) {
            throw new NullPointerException("class name must not be null.");
        }

        try {
            if (classFileBuffer == null) {
                ASMClassNodeAdapter classNode = ASMClassNodeAdapter.get(instrumentContext, classLoader, JavaAssistUtils.javaNameToJvmName(className));
                if (classNode == null) {
                    return null;
                }
                return new ASMClass(engineComponent, instrumentContext, classLoader, classNode);
            }

            // Use ASM tree api.
            final ClassReader classReader = new ClassReader(classFileBuffer);
            final ClassNode classNode = new ClassNode();
            classReader.accept(classNode, 0);


            return new ASMClass(engineComponent, instrumentContext, classLoader, classNode);
        } catch (Exception e) {
            throw new NotFoundInstrumentException(e);
        }
    }

    @Override
    public boolean hasClass(ClassLoader classLoader, String className) {
        // TODO deprecated
        return classLoader.getResource(JavaAssistUtils.javaNameToJvmName(className) + ".class") != null;
    }

    @Override
    public void appendToBootstrapClassPath(JarFile jarFile) {
        if (jarFile == null) {
            throw new NullPointerException("jarFile must not be null");
        }
        if (isInfo) {
            logger.info("appendToBootstrapClassPath:{}", jarFile);
        }
        instrumentation.appendToBootstrapClassLoaderSearch(jarFile);
    }
}