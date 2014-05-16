package com.github.lemniscate.util.bytecode;

import javassist.*;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.ArrayMemberValue;
import javassist.bytecode.annotation.MemberValue;
import javassist.bytecode.annotation.StringMemberValue;

/**
 * @Author dave 5/16/14 8:49 AM
 */
public class JavassistUtil {

    public static String getGenericSignature(ClassPool pool, Class<?> baseImpl, Class<?>... classes) throws NotFoundException {
        StringBuilder name = new StringBuilder( baseImpl.getSimpleName() );
        String baseImplSig = baseImpl.getName().replace(".", "/");

        // TODO find out why we have the root Object in there...
        StringBuilder sig = new StringBuilder("Ljava/lang/Object;L" + baseImplSig + "<");
        for(int i = 0; i < classes.length; i++){
            Class<?> c = classes[i];
            pool.appendClassPath( new ClassClassPath(c));
            CtClass ct = pool.get( c.getName() );
            sig.append("L")
                    .append( ct.getName().replace('.', '/') )
                    .append(";");

            // while we're at it, append the class types to the name
            name.append("_")
                    .append(c.getSimpleName());
        }
        sig.append(">;");
        return sig.toString();
    }

    public static Class<?> generateTypedInterface(String name, Class<?> superInterface, Class<?>... typeArgs) throws NotFoundException, CannotCompileException {
        ClassPool pool = ClassPool.getDefault();

        CtClass impl = pool.makeInterface( name );
        impl.setSuperclass( pool.get(superInterface.getName()) );
        impl.setGenericSignature( getGenericSignature(pool, superInterface, typeArgs));

        Class<?> result = impl.toClass();
        return result;
    }

    public static Class<?> generateTypedSubclass(String name, Class<?> baseImpl, Class<?>... classes) {
        try{
            ClassPool pool = ClassPool.getDefault();
            pool.appendClassPath(new ClassClassPath(baseImpl));

            // Generate our actual base class
            CtClass ctBaseImpl = pool.get(baseImpl.getName());
            CtClass impl = pool.makeClass( name.toString() );
            impl.setSuperclass(ctBaseImpl);

            // apply our generic signature
            impl.setGenericSignature( getGenericSignature(pool, baseImpl, classes) );


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




            Class<?> result = impl.toClass();
            return result;
        }catch(Exception e){
            throw new RuntimeException("Failed to create subclass: " + e.getMessage(), e);
        }
    }
}
