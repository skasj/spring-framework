/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.type.filter;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * A simple {@link TypeFilter} which matches classes with a given annotation,
 * checking inherited annotations as well.
 *
 * <p>By default, the matching logic mirrors that of
 * {@link AnnotationUtils#getAnnotation(java.lang.reflect.AnnotatedElement, Class)},
 * supporting annotations that are <em>present</em> or <em>meta-present</em> for a
 * single level of meta-annotations. The search for meta-annotations my be disabled.
 * Similarly, the search for annotations on interfaces may optionally be enabled.
 * Consult the various constructors in this class for details.
 *
 * 一个简单的TypeFilter，它将类与给定的注释相匹配，并检查继承的注释。
 * 默认情况下，匹配逻辑反映了AnnotationUtils.getAnnotation（reflect.AnnotatedElement，Class）的逻辑，
 * 支持单个级别的元注释存在或元存在的注释。元注释搜索被禁用。类似地，可以选择启用对接口上注释的搜索。
 * 有关详细信息，请咨询此类中的各种构造函数。
 *
 * @author Mark Fisher
 * @author Ramnivas Laddad
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 2.5
 */
public class AnnotationTypeFilter extends AbstractTypeHierarchyTraversingFilter {

	private final Class<? extends Annotation> annotationType;

	/**
	 * 是否考虑元注解（元注解就是注解其他注解的注解，如@Target,@Retention,@Documented,@Inherited）
	 */
	private final boolean considerMetaAnnotations;


	/**
	 * Create a new {@code AnnotationTypeFilter} for the given annotation type.
	 * <p>The filter will also match meta-annotations. To disable the
	 * meta-annotation matching, use the constructor that accepts a
	 * '{@code considerMetaAnnotations}' argument.
	 * <p>The filter will not match interfaces.
	 * @param annotationType the annotation type to match
	 */
	public AnnotationTypeFilter(Class<? extends Annotation> annotationType) {
		this(annotationType, true, false);
	}

	/**
	 * Create a new {@code AnnotationTypeFilter} for the given annotation type.
	 * <p>The filter will not match interfaces.
	 * @param annotationType the annotation type to match
	 * @param considerMetaAnnotations whether to also match on meta-annotations
	 */
	public AnnotationTypeFilter(Class<? extends Annotation> annotationType, boolean considerMetaAnnotations) {
		this(annotationType, considerMetaAnnotations, false);
	}

	/**
	 * Create a new {@code AnnotationTypeFilter} for the given annotation type.
	 * @param annotationType the annotation type to match
	 * @param considerMetaAnnotations whether to also match on meta-annotations
	 * @param considerInterfaces whether to also match interfaces
	 */
	public AnnotationTypeFilter(
			Class<? extends Annotation> annotationType, boolean considerMetaAnnotations, boolean considerInterfaces) {

		super(annotationType.isAnnotationPresent(Inherited.class), considerInterfaces);
		this.annotationType = annotationType;
		this.considerMetaAnnotations = considerMetaAnnotations;
	}

	/**
	 * Return the {@link Annotation} that this instance is using to filter
	 * candidates.
	 * @since 5.0
	 */
	public final Class<? extends Annotation> getAnnotationType() {
		return this.annotationType;
	}

	@Override
	protected boolean matchSelf(MetadataReader metadataReader) {
		// 获得注解元数据
		AnnotationMetadata metadata = metadataReader.getAnnotationMetadata();
		// 存在指定注释或注释的完全限定类名
		return metadata.hasAnnotation(this.annotationType.getName()) ||
				(this.considerMetaAnnotations && metadata.hasMetaAnnotation(this.annotationType.getName()));
	}

	@Override
	@Nullable
	protected Boolean matchSuperClass(String superClassName) {
		// 检查父类是否被目标注解声明
		return hasAnnotation(superClassName);
	}

	@Override
	@Nullable
	protected Boolean matchInterface(String interfaceName) {
		return hasAnnotation(interfaceName);
	}

	@Nullable
	protected Boolean hasAnnotation(String typeName) {
		// 避免空指针，Object.class 没有父类
		if (Object.class.getName().equals(typeName)) {

			return false;
		}
		else if (typeName.startsWith("java")) {
			// java 包路径下
			if (!this.annotationType.getName().startsWith("java")) {
				// 当前注解得同样是 Java 包下才有匹配的可能
				// Standard Java types do not have non-standard annotations on them ->
				// skip any load attempt, in particular for Java language interfaces.
				return false;
			}
			try {
				// 加载父类
				Class<?> clazz = ClassUtils.forName(typeName, getClass().getClassLoader());
				// 检查注解是否存在
				return ((this.considerMetaAnnotations ? AnnotationUtils.getAnnotation(clazz, this.annotationType) :
						clazz.getAnnotation(this.annotationType)) != null);
			}
			catch (Throwable ex) {
				// Class not regularly loadable - can't determine a match that way.
			}
		}
		// 非java 标准类不考虑
		return null;
	}

}
