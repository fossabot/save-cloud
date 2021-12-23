package org.cqfn.save.entities

import org.cqfn.save.mappers.GitMapper
import org.mapstruct.factory.Mappers
import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.OneToOne

/**
 * Data class with repository information
 * fixme should operate not with password, but with some sort of token (github integration)
 *
 * @property url url of repo
 * @property username username to credential
 * @property password password to credential
 * @property branch branch to clone
 * @property project
 */
@Entity
class Git(
    var url: String,
    var username: String? = null,
    var password: String? = null,
    var branch: String? = null,

    @OneToOne
    @JoinColumn(name = "project_id")
    var project: Project,
) : BaseEntity() {
    /**
     * @return git dto
     */
    fun toDto() = mapper.toDto(this)

    companion object {
        private val mapper = Mappers.getMapper(GitMapper::class.java)
    }
}
