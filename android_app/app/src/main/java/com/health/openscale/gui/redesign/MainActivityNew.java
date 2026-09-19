/* Copyright (C) 2026  openScale contributors
*
*    This program is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 3 of the License, or
*    (at your option) any later version.
*
*    This program is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with this program.  If not, see <http://www.gnu.org/licenses/>
*/
package com.health.openscale.gui.redesign;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.health.openscale.R;
import com.health.openscale.core.OpenScale;
import com.health.openscale.gui.MainActivity;
import com.health.openscale.gui.slides.AppIntroActivity;

/**
 * Activity da UI redesenhada. É a porta de entrada do app.
 *
 * Diferente da MainActivity antiga, não tem Toolbar nem DrawerLayout — o
 * design usa apenas a bottom nav, e cada tela traz o próprio título.
 *
 * A Activity antiga continua registrada, sem ser launcher: LegacyBridge a
 * abre para o que o design ainda não cobre (backup, lembretes, pareamento
 * de balança e edição de perfil).
 */
public class MainActivityNew extends AppCompatActivity {

    private static final int APPINTRO_REQUEST = 103;

    private NavController navController;
    private MaterialButton weighButton;

    @Override
    protected void attachBaseContext(Context context) {
        // Reaproveita a lógica de idioma da Activity antiga: é a mesma
        // preferência e não há motivo para duplicá-la.
        super.attachBaseContext(MainActivity.createBaseContext(context));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyThemeMode();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rd_activity_main);

        applyLightSystemBars();

        final BottomNavigationView bottomNav = findViewById(R.id.rd_bottom_nav);
        weighButton = findViewById(R.id.rd_weigh_button);

        navController = Navigation.findNavController(this, R.id.rd_nav_host);
        NavigationUI.setupWithNavController(bottomNav, navController);

        weighButton.setOnClickListener(v ->
                WeighSheet.newInstance().show(getSupportFragmentManager(), WeighSheet.TAG));

        // O botão "Pesar" pertence às telas de dados. Em Ajustes ele seria
        // ruído, então some.
        navController.addOnDestinationChangedListener(
                (controller, destination, arguments) -> updateWeighButton(destination));

        maybeShowOnboarding();
    }

    /**
     * Onboarding da primeira execução.
     *
     * Esta Activity é o launcher, então a responsabilidade de mostrar os
     * slides passou para cá — antes vivia na MainActivity antiga. Sem isso,
     * uma instalação nova abriria sem nenhum usuário, e o app não grava
     * medição sem usuário selecionado.
     */
    private void maybeShowOnboarding() {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        if (prefs.getBoolean("firstStart", true)) {
            prefs.edit().putBoolean("firstStart", false).apply();
            startActivityForResult(new Intent(this, AppIntroActivity.class), APPINTRO_REQUEST);
            return;
        }

        // Já viu o intro mas continua sem usuário (por exemplo, pulou os
        // slides ou apagou o único perfil): manda criar um.
        if (OpenScale.getInstance().getSelectedScaleUserId() == -1) {
            openUserCreation();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == APPINTRO_REQUEST
                && OpenScale.getInstance().getSelectedScaleUserId() == -1) {
            openUserCreation();
        }
    }

    /**
     * Criação do primeiro usuário.
     *
     * Ainda usa a tela antiga: ela já resolve validação de data, altura,
     * unidades e meta. Ver decisão §14.
     */
    private void openUserCreation() {
        LegacyBridge.openUserSettings(this, -1);
    }

    private void updateWeighButton(NavDestination destination) {
        final boolean visible = destination.getId() != R.id.rd_settings;
        weighButton.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    /**
     * Aplica claro/escuro/sistema a partir da preferência.
     *
     * Usa a mesma chave "app_theme" da UI antiga, com um valor a mais
     * ("System"), que é o padrão do design.
     */
    private void applyThemeMode() {
        final String theme = PreferenceManager.getDefaultSharedPreferences(this)
                .getString("app_theme", "System");

        if ("Dark".equals(theme)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else if ("Light".equals(theme)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    /**
     * Ícones escuros na status bar quando o tema é claro.
     *
     * Feito em código, e não por values-v23, porque o fundo do tema claro
     * (#FBF3F1) é claro demais para os ícones brancos padrão.
     */
    private void applyLightSystemBars() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }

        final int nightMode = getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        final boolean isLight =
                nightMode != android.content.res.Configuration.UI_MODE_NIGHT_YES;

        final View decor = getWindow().getDecorView();
        int flags = decor.getSystemUiVisibility();

        if (isLight) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
        } else {
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
        }

        decor.setSystemUiVisibility(flags);
    }
}
