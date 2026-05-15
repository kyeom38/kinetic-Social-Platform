package com.example.demo.repository;

import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    // email に UNIQUE 制約があるため複数ヒットは起きないが、
    // findByEmail は複数件時に例外を投げるため、呼び出し側は findFirstByEmail を使うこと。
    Optional<User> findFirstByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByEmployeeId(String employeeId);
    Optional<User> findFirstByEmployeeId(String employeeId);
    boolean existsByRole(String role);
    // 在籍中の社員のみ
    List<User> findByActiveTrueOrderByDepartmentAscNameAsc();
    // 退職済み社員（退職日の新しい順）
    List<User> findByActiveFalseOrderByRetiredAtDesc();
}
