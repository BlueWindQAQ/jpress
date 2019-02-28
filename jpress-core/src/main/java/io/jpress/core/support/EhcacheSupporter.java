/**
 * Copyright (c) 2016-2019, Michael Yang 杨福海 (fuhai999@gmail.com).
 * <p>
 * Licensed under the GNU Lesser General Public License (LGPL) ,Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.gnu.org/licenses/lgpl-3.0.txt
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.jpress.core.support;


import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 为什么需要 EhcacheSupporter ?
 * <p>
 * EhcacheSupporter 的主要作用是用于初始化 EhCache 的 CacheManager
 * 默认情况下，Ehcache 是通过 默认的 Classloader 加载数据的
 * 但是由于 JPress 内置了插件机制，所有的插件都是通过插件自己的 Classloader 进行加载
 * 这样会导致 EhCache 和 插件的 Classloader 不是通过一个 Classloader，从而导致 ClassNotFound 的异常
 * <p>
 * 所以，此类的主要作用，是保证 EhCache 用于加载缓存的 Classloader 能够找到 插件加载进来的 Class
 *
 * 另外：对于插件来说，每个插件必须使用一个新的 Classloader，才能保证 插件在后台进行 安装、卸载、停止、启用的正常工作
 * 否则当用户卸载插件后重新安装，无法加载到新的Class（之前的Class还在内存里）
 */
public class EhcacheSupporter {

    private static EhcacheClassloader ehcacheClassloader = new EhcacheClassloader();

    public static void init() {

        Configuration config = ConfigurationFactory.parseConfiguration();
        config.setClassLoader(ehcacheClassloader);
        config.setMaxBytesLocalHeap("1G");
//        config.setMaxBytesLocalOffHeap("2G");
        config.setMaxBytesLocalDisk("5G");

        CacheConfiguration cacheConfiguration = new CacheConfiguration();
        cacheConfiguration.setClassLoader(ehcacheClassloader);
        config.defaultCache(cacheConfiguration);

        CacheManager.create(config);
    }

    public static void addMapping(String className, ClassLoader classLoader) {
        ehcacheClassloader.addMapping(className, classLoader);
    }


    public static class EhcacheClassloader extends ClassLoader {

        private final static ClassLoader parent = EhcacheClassloader.class.getClassLoader();
        private final static Map<String, ClassLoader> classLoaderCache = new ConcurrentHashMap<>();

        public EhcacheClassloader() {
        }


        public void addMapping(String className, ClassLoader classLoader) {
            classLoaderCache.put(className, classLoader);
        }


        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            ClassLoader c = classLoaderCache.get(name);
            if (c == null) c = parent;
            return c.loadClass(name);
        }
    }
}
