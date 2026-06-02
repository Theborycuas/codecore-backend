/**
 * R2DBC persistence models for IAM.
 * <p>
 * Fields such as {@code email_verified} on {@link IamUserEntity} are
 * <strong>derived projections</strong> of domain state ({@link com.codecore.iam.domain.valueobject.IdentityStatus}).
 * Do not use entity getters for business rules — map to {@link com.codecore.iam.domain.model.identity.Identity}
 * via {@link com.codecore.iam.infrastructure.persistence.mapper.IamUserMapper} and use {@code identity.status()}.
 */
package com.codecore.iam.infrastructure.persistence.entity;
