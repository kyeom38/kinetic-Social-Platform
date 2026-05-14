package com.example.demo.service;

import com.example.demo.dto.UserDto;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User activeUser;
    private User retiredUser;

    @BeforeEach
    void setUp() {
        activeUser = new User();
        activeUser.setId(1L);
        activeUser.setName("田中 太郎");
        activeUser.setEmail("tanaka@example.com");
        activeUser.setRole("USER");
        activeUser.setDepartment("エンジニアリング部");
        activeUser.setJobTitle("部長");
        activeUser.setActive(true);

        retiredUser = new User();
        retiredUser.setId(2L);
        retiredUser.setName("鈴木 花子");
        retiredUser.setEmail("suzuki@example.com");
        retiredUser.setRole("USER");
        retiredUser.setActive(false);
        retiredUser.setRetiredAt(LocalDate.of(2024, 3, 31));
    }

    @Test
    @DisplayName("在籍中の社員一覧が取得できる")
    void getActiveUsers_returnsActiveMappedUsers() {
        when(userRepository.findByActiveTrueOrderByDepartmentAscNameAsc())
                .thenReturn(List.of(activeUser));

        List<UserDto> result = userService.getActiveUsers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("田中 太郎");
        assertThat(result.get(0).isActive()).isTrue();
    }

    @Test
    @DisplayName("退職済み社員一覧が取得できる")
    void getRetiredUsers_returnsMappedRetiredUsers() {
        when(userRepository.findByActiveFalseOrderByRetiredAtDesc())
                .thenReturn(List.of(retiredUser));

        List<UserDto> result = userService.getRetiredUsers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isActive()).isFalse();
        assertThat(result.get(0).getRetiredAt()).isEqualTo(LocalDate.of(2024, 3, 31));
    }

    @Test
    @DisplayName("IDで社員が取得できる")
    void getById_returnsUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));

        UserDto result = userService.getById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("田中 太郎");
    }

    @Test
    @DisplayName("存在しないIDで例外が発生する")
    void getById_throwsWhenNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found: 999");
    }

    @Test
    @DisplayName("社員作成でパスワードがエンコードされる")
    void create_encodesPasswordAndSaves() {
        UserDto dto = new UserDto();
        dto.setName("新田 一郎");
        dto.setEmail("nitta@example.com");
        dto.setPassword("RawPass1!");
        dto.setRole("USER");

        User saved = new User();
        saved.setId(10L);
        saved.setName("新田 一郎");
        saved.setEmail("nitta@example.com");
        saved.setRole("USER");
        saved.setActive(true);

        when(passwordEncoder.encode("RawPass1!")).thenReturn("$encoded$");
        when(userRepository.save(any(User.class))).thenReturn(saved);

        UserDto result = userService.create(dto);

        verify(passwordEncoder).encode("RawPass1!");
        verify(userRepository).save(argThat(u -> "$encoded$".equals(u.getPassword())));
        assertThat(result.getId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("退職処理でactive=falseとretiredAtが設定される")
    void retire_setsInactiveAndRetiredAt() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(userRepository.save(any(User.class))).thenReturn(activeUser);

        userService.retire(1L, java.time.LocalDate.of(2026, 3, 31));

        verify(userRepository).save(argThat(u -> !u.isActive() && u.getRetiredAt() != null));
    }

    @Test
    @DisplayName("社員自身のプロフィール更新で名前・部署は変更されない")
    void update_doesNotChangeNameOrDepartment() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(userRepository.save(any(User.class))).thenReturn(activeUser);

        UserDto dto = new UserDto();
        dto.setName("別の名前");           // 無視されるべき
        dto.setDepartment("別の部署");     // 無視されるべき
        dto.setNickname("たろう");
        dto.setBio("よろしく");

        userService.update(1L, dto);

        verify(userRepository).save(argThat(u ->
                "田中 太郎".equals(u.getName()) &&
                "エンジニアリング部".equals(u.getDepartment()) &&
                "たろう".equals(u.getNickname())
        ));
    }

    @Test
    @DisplayName("管理者による編集で名前・部署・役職が更新される")
    void adminUpdate_updatesAdminFields() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(userRepository.save(any(User.class))).thenReturn(activeUser);

        UserDto dto = new UserDto();
        dto.setName("田中 二郎");
        dto.setDepartment("マーケティング部");
        dto.setJobTitle("課長");
        dto.setRole("USER");

        userService.adminUpdate(1L, dto);

        verify(userRepository).save(argThat(u ->
                "田中 二郎".equals(u.getName()) &&
                "マーケティング部".equals(u.getDepartment()) &&
                "課長".equals(u.getJobTitle())
        ));
    }

    @Test
    @DisplayName("組織図が部署ごとに部長/メンバーで分類される")
    void getOrgChart_groupsByDepartmentAndRole() {
        User manager = new User();
        manager.setId(1L); manager.setName("山田 部長"); manager.setDepartment("営業部");
        manager.setJobTitle("部長"); manager.setActive(true);

        User member = new User();
        member.setId(2L); member.setName("佐藤 一般"); member.setDepartment("営業部");
        member.setJobTitle("一般社員"); member.setActive(true);

        when(userRepository.findByActiveTrueOrderByDepartmentAscNameAsc())
                .thenReturn(List.of(manager, member));

        var orgChart = userService.getOrgChart();

        assertThat(orgChart).containsKey("営業部");
        assertThat(orgChart.get("営業部").get("managers")).hasSize(1);
        assertThat(orgChart.get("営業部").get("members")).hasSize(1);
        assertThat(orgChart.get("営業部").get("managers").get(0).getName()).isEqualTo("山田 部長");
    }

    @Test
    @DisplayName("プロフィール写真のURLが更新される")
    void updatePhoto_savesPhotoUrl() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(userRepository.save(any(User.class))).thenReturn(activeUser);

        userService.updatePhoto(1L, "/uploads/photo.jpg");

        verify(userRepository).save(argThat(u -> "/uploads/photo.jpg".equals(u.getProfileImageUrl())));
    }
}
