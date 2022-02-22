package org.xwiki.model.reference;

/**
 * @deprecated since 5.4
 */
@Deprecated
public interface ImmutableReference {

  /**
   * @deprecated since 5.4, EntityReference is inherently immutable
   */
  @Deprecated
  EntityReference getMutable();

}
