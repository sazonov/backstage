package com.proit.app.database.model;

import javax.persistence.Entity;

/**
 * Добавлена в качестве единственной сущности для обеспечения корректной работы weaving у eclipselink.
 * @see UuidGeneratedEntity
 */
@Entity
class EmptyEntity extends UuidGeneratedEntity
{
}
