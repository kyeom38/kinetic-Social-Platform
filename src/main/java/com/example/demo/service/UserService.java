package com.example.demo.service;

import com.example.demo.dto.UserDto;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public List<UserDto> getActiveUsers() {
        return userRepository.findByActiveTrueOrderByDepartmentAscNameAsc()
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<UserDto> getRetiredUsers() {
        return userRepository.findByActiveFalseOrderByRetiredAtDesc()
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public Map<String, Map<String, List<UserDto>>> getOrgChart() {
        var users = userRepository.findByActiveTrueOrderByDepartmentAscNameAsc().stream()
                .filter(u -> u.getDepartment() != null && !u.getDepartment().isEmpty())
                .map(this::toDto)
                .collect(Collectors.toList());

        Map<String, Map<String, List<UserDto>>> result = new LinkedHashMap<>();
        for (UserDto u : users) {
            boolean isManager = "部長".equals(u.getJobTitle()) || "課長".equals(u.getJobTitle());
            result.computeIfAbsent(u.getDepartment(), k -> new LinkedHashMap<>())
                  .computeIfAbsent(isManager ? "managers" : "members", k -> new ArrayList<>())
                  .add(u);
        }
        return result;
    }

    public UserDto create(UserDto dto) {
        User user = toEntity(dto);
        if (dto.getEmail() != null && !dto.getEmail().isEmpty()) {
            user.setEmail(dto.getEmail());
            user.setPassword(passwordEncoder.encode(
                    // パスワード未指定時のフォールバック。管理者フォームは required のため通常は発生しない。
                    dto.getPassword() != null ? dto.getPassword() : "ChangeMe1!"));
            user.setRole(dto.getRole() != null ? dto.getRole() : "USER");
        }
        user.setActive(true);
        return toDto(userRepository.save(user));
    }

    /** 管理者によるアカウント作成。初回ログイン時にパスワード変更を強制する。 */
    public UserDto createWithMustChangePassword(UserDto dto) {
        User user = toEntity(dto);
        if (dto.getEmail() != null && !dto.getEmail().isEmpty()) {
            user.setEmail(dto.getEmail());
            user.setPassword(passwordEncoder.encode(
                    dto.getPassword() != null ? dto.getPassword() : "ChangeMe1!"));
            user.setRole(dto.getRole() != null ? dto.getRole() : "USER");
        }
        user.setActive(true);
        user.setMustChangePassword(true);
        return toDto(userRepository.save(user));
    }

    /** パスワード変更後に強制変更フラグを解除する */
    public void clearMustChangePassword(Long id) {
        userRepository.findById(id).ifPresent(u -> {
            u.setMustChangePassword(false);
            userRepository.save(u);
        });
    }

    public UserDto getById(Long id) {
        return toDto(userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id)));
    }

    public UserDto getByEmail(String email) {
        return userRepository.findFirstByEmail(email).map(this::toDto).orElse(null);
    }

    // ─────────────────────────────────────────────────────────────────────
    // ソフトデリート: 退職処理（物理削除しない）
    // ─────────────────────────────────────────────────────────────────────
    public void retire(Long id, LocalDate retiredAt) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User not found: id={}", id);
                    return new RuntimeException("User not found: " + id);
                });
        user.setActive(false);
        user.setRetiredAt(retiredAt);
        userRepository.save(user);
    }

    /**
     * 社員自身によるプロフィール編集（自己編集）
     * employeeId / department / jobTitle は変更不可（フォームから渡ってきた値は無視して既存値を維持）
     */
    public UserDto update(Long id, UserDto dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User not found: id={}", id);
                    return new RuntimeException("User not found: " + id);
                });

        // 名前・生年月日・profileImageUrl は管理者 or 写真専用エンドポイントでのみ変更可
        user.setNickname(dto.getNickname());
        user.setBio(dto.getBio());
        user.setHobbies(dto.getHobbies());
        user.setCommunicationStyle(dto.getCommunicationStyle());
        user.setCompanyBelief(dto.getCompanyBelief());
        user.setMbti(dto.getMbti());
        // employeeId・department・jobTitle は社員本人は変更不可（管理者のみ）

        return toDto(userRepository.save(user));
    }

    /**
     * 管理者による社員情報の編集（管理者専用）
     * 変更できる項目: 名前、社員番号、部署、役職、権限
     */
    public UserDto adminUpdate(Long id, UserDto dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User not found: id={}", id);
                    return new RuntimeException("User not found: " + id);
                });
        user.setName(dto.getName());
        user.setEmployeeId(dto.getEmployeeId());
        user.setDepartment(dto.getDepartment());
        user.setJobTitle(dto.getJobTitle());
        user.setBirthDate(dto.getBirthDate());
        if (dto.getRole() != null && !dto.getRole().isEmpty()) {
            user.setRole(dto.getRole());
        }
        return toDto(userRepository.save(user));
    }

    /** メールアドレスでパスワードを検証（退職確認など用） */
    public boolean verifyPassword(String email, String rawPassword) {
        return userRepository.findFirstByEmail(email)
                .map(u -> passwordEncoder.matches(rawPassword, u.getPassword()))
                .orElse(false);
    }

    /** パスワード変更（現在のパスワードが一致する場合のみ） */
    public boolean changePassword(Long id, String currentRaw, String newRaw) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User not found: id={}", id);
                    return new RuntimeException("User not found: " + id);
                });
        if (!passwordEncoder.matches(currentRaw, user.getPassword())) {
            log.warn("Password mismatch on change attempt: userId={}", id);
            return false;
        }
        user.setPassword(passwordEncoder.encode(newRaw));
        userRepository.save(user);
        return true;
    }

    /** 管理者によるパスワード強制リセット */
    public void resetPassword(Long id, String newRaw) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User not found: id={}", id);
                    return new RuntimeException("User not found: " + id);
                });
        user.setPassword(passwordEncoder.encode(newRaw));
        userRepository.save(user);
    }

    /** プロフィール写真だけを更新する */
    public void updatePhoto(Long id, String photoUrl) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User not found: id={}", id);
                    return new RuntimeException("User not found: " + id);
                });
        user.setProfileImageUrl(photoUrl);
        userRepository.save(user);
    }

    private User toEntity(UserDto dto) {
        User user = new User();
        user.setName(dto.getName());
        user.setNickname(dto.getNickname());
        user.setEmployeeId(dto.getEmployeeId());
        user.setBirthDate(dto.getBirthDate());
        user.setProfileImageUrl(dto.getProfileImageUrl());
        user.setBio(dto.getBio());
        user.setHobbies(dto.getHobbies());
        user.setCommunicationStyle(dto.getCommunicationStyle());
        user.setCompanyBelief(dto.getCompanyBelief());
        user.setDepartment(dto.getDepartment());
        user.setJobTitle(dto.getJobTitle());
        user.setMbti(dto.getMbti());
        return user;
    }

    private UserDto toDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setNickname(user.getNickname());
        dto.setEmployeeId(user.getEmployeeId());
        dto.setBirthDate(user.getBirthDate());
        dto.setProfileImageUrl(user.getProfileImageUrl());
        dto.setBio(user.getBio());
        dto.setHobbies(user.getHobbies());
        dto.setCommunicationStyle(user.getCommunicationStyle());
        dto.setCompanyBelief(user.getCompanyBelief());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        dto.setDepartment(user.getDepartment());
        dto.setJobTitle(user.getJobTitle());
        dto.setMbti(user.getMbti());
        dto.setActive(user.isActive());
        dto.setRetiredAt(user.getRetiredAt());
        dto.setJoinedAt(user.getJoinedAt());
        return dto;
    }
}
