import com.github.lemniscate.util.bytecode.JavassistUtil;
import org.junit.Test;
import org.springframework.core.GenericTypeResolver;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

public class InterfaceTest {
    @Test
    public void makeTypedInterface() throws Exception{
//        ClassPool pool = ClassPool.getDefault();
//        CtClass impl = pool.makeInterface("Test");
//        impl.setSuperclass( pool.get(Service.class.getName()) );
//        impl.setGenericSignature( JavassistUtil.getGenericSignature( pool, Service.class, String.class));
//
//        Class<?> result = impl.toClass();
        Class<?> result = JavassistUtil.generateTypedInterface("Test", Service.class, String.class);
        assertNotNull(result);

        Class<?>[] typeArgs = GenericTypeResolver.resolveTypeArguments(result, Service.class);
        assertNotNull(typeArgs);
        assertTrue(typeArgs.length > 0);
    }


    public static interface Service<T> {
        T get();
    }
}

