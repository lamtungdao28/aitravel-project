package com.travelhub.user.repository;
import com.travelhub.user.entity.*;
import java.util.List;
import org.springframework.data.jpa.repository.*;
public interface RolePermissionRepository extends JpaRepository<RolePermission,RolePermissionId> {
    @Query("select rp.permission.name from RolePermission rp where rp.role.id=:roleId")
    List<String> findPermissionNames(Long roleId);
    List<RolePermission> findByRoleId(Long roleId);
}
