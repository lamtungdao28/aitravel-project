package com.travelhub.user.repository;

import com.travelhub.user.entity.*;

import org.springframework.data.jpa.repository.*;

import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {
    @Query("select rp.permission.name from RolePermission rp where rp.role.id=:roleId")
    List<String> findPermissionNames(Long roleId);

    List<RolePermission> findByRoleId(Long roleId);
}
