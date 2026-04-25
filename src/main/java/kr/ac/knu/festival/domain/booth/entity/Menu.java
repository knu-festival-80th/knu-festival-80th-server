package kr.ac.knu.festival.domain.booth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import kr.ac.knu.festival.global.base.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "menu")
@SQLDelete(sql = "UPDATE menu SET deleted_at = CURRENT_TIMESTAMP WHERE menu_id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Menu extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "menu_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booth_id", nullable = false)
    private Booth booth;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private int price;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_sold_out", nullable = false)
    private boolean soldOut;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static Menu createMenu(
            Booth booth,
            String name,
            int price,
            String imageUrl,
            String description
    ) {
        return Menu.builder()
                .booth(booth)
                .name(name)
                .price(price)
                .imageUrl(imageUrl)
                .description(description)
                .soldOut(false)
                .build();
    }

    public void updateMenu(String name, Integer price, String imageUrl, String description) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (price != null && price >= 0) {
            this.price = price;
        }
        if (imageUrl != null) {
            this.imageUrl = imageUrl;
        }
        if (description != null) {
            this.description = description;
        }
    }

    public void toggleSoldOut() {
        this.soldOut = !this.soldOut;
    }
}
