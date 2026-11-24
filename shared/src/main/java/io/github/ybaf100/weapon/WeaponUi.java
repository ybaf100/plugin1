package io.github.ybaf100.weapon;

import org.bukkit.entity.Player;

/** 버전별 메뉴 화면. 1.19.4는 상자 GUI, 최신 버전은 대화창(Dialog). */
public interface WeaponUi {

    /** 플러그인이 켜질 때 (리스너 등록 등). */
    void enable();

    /** 플러그인이 꺼질 때 열린 메뉴를 닫는다. */
    void disable();

    /** /무기 메인 메뉴. */
    void openMain(Player player);
}
