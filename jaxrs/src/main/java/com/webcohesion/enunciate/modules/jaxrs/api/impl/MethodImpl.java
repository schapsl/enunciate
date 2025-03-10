/**
 * Copyright © 2006-2016 Web Cohesion (info@webcohesion.com)
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
package com.webcohesion.enunciate.modules.jaxrs.api.impl;

import com.webcohesion.enunciate.api.ApiRegistrationContext;
import com.webcohesion.enunciate.api.Styles;
import com.webcohesion.enunciate.api.resources.*;
import com.webcohesion.enunciate.api.resources.Resource;
import com.webcohesion.enunciate.facets.Facet;
import com.webcohesion.enunciate.javac.decorations.element.ElementUtils;
import com.webcohesion.enunciate.javac.javadoc.JavaDoc;
import com.webcohesion.enunciate.metadata.swagger.OperationId;
import com.webcohesion.enunciate.modules.jaxrs.model.*;
import io.swagger.annotations.ApiOperation;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.VariableElement;
import java.lang.annotation.Annotation;
import java.util.*;

/**
 * @author Ryan Heaton
 */
public class MethodImpl implements Method {

  private static final com.webcohesion.enunciate.modules.jaxrs.model.util.MediaType MULTIPART_FORM_DATA = new com.webcohesion.enunciate.modules.jaxrs.model.util.MediaType("multipart/form-data", 1.0F);
  private final String httpMethod;
  private final ResourceMethod resourceMethod;
  private final ResourceGroup group;
  private final MethodExampleImpl example;
  private final ApiRegistrationContext registrationContext;

  public MethodImpl(String httpMethod, ResourceMethod resourceMethod, ResourceGroup group, ApiRegistrationContext registrationContext) {
    this.httpMethod = httpMethod;
    this.resourceMethod = resourceMethod;
    this.group = group;
    this.registrationContext = registrationContext;
    this.example = this.resourceMethod.getContext().isDisableExamples() ? null : new MethodExampleImpl(this.httpMethod, this.resourceMethod, this.registrationContext);
  }

  @Override
  public Resource getResource() {
    return new ResourceImpl(this.resourceMethod, this.group, registrationContext);
  }

  @Override
  public String getLabel() {
    return this.resourceMethod.getLabel() == null ? this.httpMethod : this.resourceMethod.getLabel();
  }

  @Override
  public String getDeveloperLabel() {
    OperationId operationId = this.resourceMethod.getAnnotation(OperationId.class);
    if (operationId != null) {
      return operationId.value();
    }
    return this.resourceMethod.getSimpleName().toString();
  }

  @Override
  public String getHttpMethod() {
    return this.httpMethod;
  }

  @Override
  public String getSlug() {
    StringBuilder params = new StringBuilder();
    for (VariableElement parameter : resourceMethod.getParameters()) {
      params.append('_');
      params.append(parameter.getSimpleName().toString());
    }
    return this.group.getSlug() + "_" + resourceMethod.getSlug() + params + "_" + this.httpMethod;
  }

  @Override
  public String getSummary() {
    ApiOperation apiOperation = this.resourceMethod.getAnnotation(ApiOperation.class);
    if (apiOperation != null) {
      return apiOperation.value();
    }
    return null;
  }

  @Override
  public String getDescription() {
    ApiOperation apiOperation = this.resourceMethod.getAnnotation(ApiOperation.class);
    if (apiOperation != null && !apiOperation.notes().isEmpty()) {
      return apiOperation.notes();
    }

    return this.resourceMethod.getJavaDoc(this.registrationContext.getTagHandler()).toString();
  }

  @Override
  public String getDeprecated() {
    String deprecated = ElementUtils.findDeprecationMessage(this.resourceMethod, this.registrationContext.getTagHandler());
    if (deprecated == null) {
      deprecated = ElementUtils.findDeprecationMessage(this.resourceMethod.getParent(), this.registrationContext.getTagHandler());
    }
    return deprecated;
  }

  @Override
  public String getSince() {
    JavaDoc.JavaDocTagList tags = this.resourceMethod.getJavaDoc(this.registrationContext.getTagHandler()).get("since");
    return tags == null ? null : tags.toString();
  }

  @Override
  public List<String> getSeeAlso() {
    return this.resourceMethod.getJavaDoc(this.registrationContext.getTagHandler()).get("see");
  }

  @Override
  public String getVersion() {
    JavaDoc.JavaDocTagList tags = this.resourceMethod.getJavaDoc(this.registrationContext.getTagHandler()).get("version");
    return tags == null ? null : tags.toString();
  }

  @Override
  public boolean isIncludeDefaultParameterValues() {
    for (ResourceParameter parameter : this.resourceMethod.getResourceParameters()) {
      if (parameter.getDefaultValue() != null) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isHasParameterConstraints() {
    for (Parameter parameter : getParameters()) {
      if (parameter.getConstraints() != null) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isHasParameterMultiplicity() {
    for (ResourceParameter parameter : this.resourceMethod.getResourceParameters()) {
      if (parameter.isMultivalued()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public List<? extends Parameter> getParameters() {
    Set<ResourceParameter> resourceParams = this.resourceMethod.getResourceParameters();
    ArrayList<Parameter> parameters = new ArrayList<Parameter>(resourceParams.size());
    for (ResourceParameter param : resourceParams) {
      parameters.add(new ParameterImpl(param, registrationContext));
    }
    return parameters;
  }

  @Override
  public Entity getRequestEntity() {
    ResourceEntityParameter entityParameter = this.resourceMethod.getEntityParameter();
    if (entityParameter != null || this.resourceMethod.getConsumesMediaTypes().contains(MULTIPART_FORM_DATA)) {
      return new RequestEntityImpl(this.resourceMethod, entityParameter, registrationContext);
    }
    return null;
  }

  @Override
  public List<? extends StatusCode> getResponseCodes() {
    ArrayList<StatusCode> statusCodes = new ArrayList<StatusCode>();
    for (ResponseCode responseCode : this.resourceMethod.getStatusCodes()) {
      statusCodes.add(new StatusCodeImpl(responseCode, this.registrationContext));
    }
    return statusCodes;
  }

  @Override
  public Entity getResponseEntity() {
    ResourceRepresentationMetadata responseMetadata = this.resourceMethod.getRepresentationMetadata();
    return responseMetadata == null ? null : new ResponseEntityImpl(this.resourceMethod, responseMetadata, registrationContext);
  }

  @Override
  public List<? extends StatusCode> getWarnings() {
    ArrayList<StatusCode> statusCodes = new ArrayList<StatusCode>();
    for (ResponseCode responseCode : this.resourceMethod.getWarnings()) {
      statusCodes.add(new StatusCodeImpl(responseCode, this.registrationContext));
    }
    return statusCodes;
  }

  @Override
  public List<? extends Parameter> getResponseHeaders() {
    Map<String, String> responseHeaders = this.resourceMethod.getResponseHeaders();
    ArrayList<Parameter> headerValues = new ArrayList<Parameter>();
    for (Map.Entry<String, String> responseHeader : responseHeaders.entrySet()) {
      headerValues.add(new ResponseHeaderParameterImpl(responseHeader.getKey(), responseHeader.getValue(), Collections.<String>emptySet()));
    }
    return headerValues;
  }

  @Override
  public Set<String> getSecurityRoles() {
    return this.resourceMethod.getSecurityRoles();
  }

  @Override
  public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
    return this.resourceMethod.getAnnotation(annotationType);
  }

  @Override
  public Map<String, AnnotationMirror> getAnnotations() {
    return this.resourceMethod.getAnnotations();
  }

  @Override
  public Set<Facet> getFacets() {
    return this.resourceMethod.getFacets();
  }

  @Override
  public JavaDoc getJavaDoc() {
    return this.resourceMethod.getJavaDoc(this.registrationContext.getTagHandler());
  }

  @Override
  public Set<String> getStyles() {
    return Styles.gatherStyles(this.resourceMethod, this.resourceMethod.getContext().getContext().getConfiguration().getAnnotationStyles());
  }

  @Override
  public Example getExample() {
    return example;
  }

  public ResourceMethod getResourceMethod() {
    return resourceMethod;
  }
}
