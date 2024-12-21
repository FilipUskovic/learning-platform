package com.micro.learningplatform.models;

import com.micro.learningplatform.security.AuthProvider;
import com.micro.learningplatform.security.UserRole;
import com.micro.learningplatform.shared.utils.JsonbConverter;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.Type;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "users",
        indexes = {
                @Index(name = "idx_user_email", columnList = "email"),
                @Index(name = "idx_user_provider_id", columnList = "provider, provider_id")
        }
)
//@TypeDef(name = "json", typeClass = JsonType.class)

@Getter
@Setter
@NoArgsConstructor
@ToString
public class User extends BaseModel implements UserDetails, OAuth2User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(unique = true, nullable = false)
    @Email(message = "Email should be valid")
    private String email;

    @Column(nullable = true)
    private String password; // Nullable jer OAuth2 korisnici nemaju lozinku

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider = AuthProvider.LOCAL;

    @Column(name = "provider_id")
    private String providerId;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "email_verified")
    private boolean emailVerified = false;


    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Enumerated(EnumType.STRING)
    private Set<UserRole> roles = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private Set<UserToken> tokens = new HashSet<>();

    // OAuth2 specifična polja
   // @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    @Convert(converter = JsonbConverter.class)
    @ColumnTransformer(write = "?::jsonb") // Explicitly cast to jsonb
    private Map<String, Object> attributes = new HashMap<>();


    // Factory metode za kreiranje korisnika
    public static User createLocalUser(String email, String password, String firstName, String lastName ){
        User user = new User();
        user.setEmail(email);
        user.setPassword(password);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setProvider(AuthProvider.LOCAL);
        user.addRole(UserRole.USER);
        return user;
    }

    // ako zelim da admin odmah kreira i role
    public static User createUserWithRoles(String email, String password, String firstName, String lastName, AuthProvider provider, UserRole... roles) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(password);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setProvider(provider);
        user.setEmailVerified(true);
        user.setEnabled(true);
        user.getRoles().addAll(Arrays.asList(roles));
        return user;
    }

    public static User createOAuth2User(String email, String firstName, String lastName,
                                        AuthProvider provider, String providerId) {
        User user = new User();
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setProvider(provider);
        user.setProviderId(providerId);
        user.setEmailVerified(true);
        user.addRole(UserRole.USER);
        return user;
    }


    @Override
    public String getName() {
        return email;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .collect(Collectors.toSet());
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return enabled;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return enabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    // Helper metode
    public void addRole(UserRole role) {
        this.roles.add(role);
    }

    public void addToken(UserToken token) {
        tokens.add(token);
        token.setUser(this);
    }

    public void removeToken(UserToken token) {
        tokens.remove(token);
        token.setUser(null);
    }

    public void deactivateAccount() {
        this.enabled = false;
        this.tokens.forEach(token -> token.setRevoked(true));
    }

    public void verifyEmail() {
        this.emailVerified = true;
    }

    public void updateProfile(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;

    }

    public void removeRole(UserRole role) {
        this.roles.remove(role);
    }


    public boolean hasRole(UserRole role) {
        return roles.contains(role);
    }
}
