package com.github.lemniscate.util.bytecode;

import javassist.*;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.ArrayMemberValue;
import javassist.bytecode.annotation.MemberValue;
import javassist.bytecode.annotation.StringMemberValue;
import sun.reflect.generics.factory.CoreReflectionFactory;
import sun.reflect.generics.repository.ClassRepository;
import sun.reflect.generics.scope.ClassScope;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @Author dave 5/16/14 8:49 AM
 */
public class JavassistUtil {

    public static String getGenericSignature(ClassPool pool, Class<?> baseImpl, Collection<Class<?>> interfaces, Class<?>... classes) throws NotFoundException {
        StringBuilder name = new StringBuilder( baseImpl.getSimpleName() );
        String superclass = baseImpl.getName().replace(".", "/");
        String genericSuperclass = baseImpl.getName().replace(".", "/");
        StringBuilder sig = new StringBuilder("L" + superclass + ";L" + genericSuperclass + "<");

        StringBuilder types = new StringBuilder();

        for(int i = 0; i < classes.length; i++){
            Class<?> c = classes[i];
            pool.appendClassPath( new ClassClassPath(c));
            CtClass ct = pool.get( c.getName() );
            types.append("L")
                    .append( ct.getName().replace('.', '/') )
                    .append(";");

            // while we're at it, append the class types to the name
            name.append("_")
                    .append(c.getSimpleName());
        }

        sig.append(types.toString()).append(">;");

        for(Class<?> iface : interfaces){
            // TODO how do we know the right params / order / etc
            if( iface.isAssignableFrom(baseImpl) && iface.getTypeParameters().length == baseImpl.getTypeParameters().length){
                String ifaceSig = iface.getName().replace(".", "/");
                sig.append("L" + ifaceSig + "<")
                        .append(types.toString())
                        .append(">;");
            }
        }


        return sig.toString();
    }

    public static Class<?> generateTypedInterface(String name, Class<?> superInterface, Class<?>... typeArgs) throws NotFoundException, CannotCompileException {
        ClassPool pool = ClassPool.getDefault();

        pool.insertClassPath( new ClassClassPath(superInterface));
        for(Class<?> c : typeArgs){
            pool.insertClassPath( new ClassClassPath(c));
        }

        CtClass impl = pool.makeInterface( name );
        impl.setSuperclass( pool.get(superInterface.getName()) );
        impl.setGenericSignature( getGenericSignature(pool, superInterface, new ArrayList<Class<?>>(), typeArgs));

        Class<?> result = impl.toClass();
        return result;
    }

    public static Class<?> generateTypedSubclass(String name, Class<?> baseImpl, Class<?>... classes) {
        return generateTypedSubclass(name, baseImpl, new ArrayList<Class<?>>(), classes);
    }

    public static Class<?> generateTypedSubclass(String name, Class<?> baseImpl, Collection<Class<?>> interfaces, Class<?>... classes) {
        try{
            ClassPool pool = ClassPool.getDefault();
            pool.appendClassPath(new ClassClassPath(baseImpl));

            // Generate our actual base class
            CtClass ctBaseImpl = pool.get(baseImpl.getName());
            CtClass impl = pool.makeClass( name.toString() );
            impl.setSuperclass(ctBaseImpl);

            for( Class<?> iface : interfaces){
                impl.addInterface(pool.get(iface.getName()));
            }


            // apply our generic signature
            impl.setGenericSignature( getGenericSignature(pool, baseImpl, interfaces, classes) );


            CtConstructor[] constructors = ctBaseImpl.getConstructors();
            if( constructors == null ||  constructors.length == 0){
                // Add a default constructor
                CtConstructor constructor = CtNewConstructor.defaultConstructor(impl);
                constructor.setBody("{}");
                impl.addConstructor(constructor);
            }else{
                for(CtConstructor c : constructors){
                    CtNewConstructor.copy(c, ctBaseImpl, null);
                }
            }

            CtMethod m = CtNewMethod.make(
                    "public java.lang.reflect.TypeVariable[] getTypeParameters() { throw new RuntimeException(); }",
                    impl);
            impl.addMethod(m);


            Class<?> result = impl.toClass();

            return result;
        }catch(Exception e){
            throw new RuntimeException("Failed to create subclass: " + e.getMessage(), e);
        }
    }
}
