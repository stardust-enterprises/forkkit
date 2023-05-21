package cpw.mods.modlauncher;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author xtrm
 */
public class FukkitTransformationServicesHandler extends TransformationServicesHandler {
    private static TransformStore transformStore;
    private Map<String, TransformationServiceDecorator> serviceLookup;

    public FukkitTransformationServicesHandler(Object delegate) {
        super(fetchTransformStore(delegate));

        for (String fieldName : new String[]{"transformationServices", "serviceLookup"}) {
            try {
                Field field = TransformationServicesHandler.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                Object value = field.get(delegate);
                if (value instanceof Map) {
                    //noinspection unchecked
                    serviceLookup = (Map<String, TransformationServiceDecorator>) value;
                }
                field.set(this, value);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static TransformStore fetchTransformStore(Object delegate) {
        if (delegate.getClass() != TransformationServicesHandler.class) {
            throw new IllegalArgumentException("delegate is not an instance of TransformationServicesHandler");
        }

        if (transformStore != null) {
            throw new UnsupportedOperationException("Fukkit only supports one instance of TransformationServicesHandler");
        }
        try {
            Field field = TransformationServicesHandler.class.getDeclaredField("transformStore");
            field.setAccessible(true);
            return transformStore = (TransformStore) field.get(delegate);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    TransformingClassLoader buildTransformingClassLoader(LaunchPluginHandler pluginHandler, TransformingClassLoaderBuilder builder, Environment environment) {
        final List<Function<String, Optional<URL>>> classLocatorList = serviceLookup.values().stream().map(TransformationServiceDecorator::getClassLoader).filter(Objects::nonNull).collect(Collectors.toList());
        Function<String, Enumeration<URL>> resourceEnumeratorLocator = builder.getResourceEnumeratorLocator();

        for (Function<String, Optional<URL>> transformerClassLocator : classLocatorList) {
            resourceEnumeratorLocator = EnumerationHelper.mergeFunctors(resourceEnumeratorLocator, EnumerationHelper.fromOptional(transformerClassLocator));
        }

        builder.setResourceEnumeratorLocator(resourceEnumeratorLocator);
        return new FukkitTransformingClassLoader(transformStore, pluginHandler, builder, environment); // Fukkit - use our own classloader
    }
}
