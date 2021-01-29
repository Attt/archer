package com.github.attt.archer.loader;


import com.github.attt.archer.invoker.context.InvocationContext;

import java.util.List;
import java.util.Map;

/**
 * Multi Loader
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public interface MultipleLoader<V> extends Loader<List<InvocationContext>, Map<InvocationContext, V>> {

}
