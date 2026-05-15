package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String nickname;
    private String employeeId;
    private LocalDate birthDate;
    private String profileImageUrl;
    private String bio;

    @ElementCollection
    @CollectionTable(name = "user_hobbies", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "hobby")
    private List<String> hobbies;

    private String communicationStyle;
    private String companyBelief;

    @Column(unique = true)
    private String email;

    private String password;

    private String role; // "ADMIN" or "USER"

    private String department;

    /** 役職: 部長, 課長, 主任, 一般社員, インターン など */
    private String jobTitle;

    /** MBTI性格タイプ (例: INTJ, ENFP) */
    private String mbti;

    // ─────────────────────────────────────────────────────────
    // ソフトデリート用フィールド
    // 退職処理ではDBから物理削除せず、active=false + retiredAt を設定する。
    // これにより退職後もポスト・コメントの作成者名が保持される（1年間）。
    // ─────────────────────────────────────────────────────────

    /** 在籍フラグ: true=在籍中, false=退職済み */
    private boolean active = true;

    /** 退職日 (退職処理時に設定) */
    private LocalDate retiredAt;

    /** true の間は全画面でパスワード変更ページへリダイレクトされる。管理者作成時に true に設定される。 */
    private boolean mustChangePassword = false;

    /** アカウント作成日（組織図の「NEW」バッジ判定に使用） */
    private LocalDate joinedAt;

    @PrePersist
    public void prePersist() {
        if (this.joinedAt == null) this.joinedAt = LocalDate.now();
    }
}
