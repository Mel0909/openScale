/* Copyright (C) 2019  olie.xdev <olie.xdev@googlemail.com>
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
package com.health.openscale.gui.slides;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.health.openscale.R;
import com.health.openscale.core.OpenScale;
import com.health.openscale.core.datatypes.ScaleUser;

import java.util.List;
import java.util.Locale;

public class UserIntroSlide extends Fragment{

    private static final String ARG_LAYOUT_RES_ID = "layoutResId";
    private int layoutResId;
    private Button btnAddUser;
    private TableLayout tblUsers;

    public static UserIntroSlide newInstance(int layoutResId) {
        UserIntroSlide sampleSlide = new UserIntroSlide();

        Bundle args = new Bundle();
        args.putInt(ARG_LAYOUT_RES_ID, layoutResId);
        sampleSlide.setArguments(args);

        return sampleSlide;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null && getArguments().containsKey(ARG_LAYOUT_RES_ID)) {
            layoutResId = getArguments().getInt(ARG_LAYOUT_RES_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(layoutResId, container, false);

        btnAddUser = view.findViewById(R.id.btnAddUser);
        tblUsers = view.findViewById(R.id.tblUsers);

        btnAddUser.setOnClickListener(new onBtnAddUserClickListener());

        updateTableUsers();

        return view;
    }

    private class onBtnAddUserClickListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            Intent intent = new Intent(getContext(), SlideToNavigationAdapter.class);
            intent.putExtra(SlideToNavigationAdapter.EXTRA_MODE, SlideToNavigationAdapter.EXTRA_USER_SETTING_MODE);
            startActivityForResult(intent, 100);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        updateTableUsers();
    }


    /**
     * Preenche a lista de perfis criados.
     *
     * Usa o mesmo item de usuário das telas novas (rd_item_user), em vez da
     * tabela com cabeçalho em negrito da versão antiga — assim o guia inicial
     * mostra o perfil do mesmo jeito que o app vai mostrar depois.
     */
    private void updateTableUsers() {
        tblUsers.removeAllViews();

        final List<ScaleUser> scaleUserList = OpenScale.getInstance().getScaleUserList();
        final LayoutInflater inflater = LayoutInflater.from(getContext());

        if (scaleUserList.isEmpty()) {
            final TextView empty = new TextView(getContext());
            empty.setText(R.string.rd_slide_no_person_yet);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, 32, 0, 32);
            tblUsers.addView(empty);
            return;
        }

        for (ScaleUser scaleUser : scaleUserList) {
            final View row = inflater.inflate(R.layout.rd_item_user, tblUsers, false);

            final TextView initial = row.findViewById(R.id.rd_user_row_initial);
            final TextView name = row.findViewById(R.id.rd_user_row_name);
            final TextView subtitle = row.findViewById(R.id.rd_user_row_subtitle);

            final String userName = scaleUser.getUserName();
            name.setText(userName);
            initial.setText(userName.isEmpty()
                    ? "" : userName.substring(0, 1).toUpperCase(Locale.getDefault()));

            // "35 anos · Feminino"
            subtitle.setText(getString(
                    scaleUser.getGender().isMale() ? R.string.label_male : R.string.label_female)
                    + " · " + scaleUser.getAge());

            row.findViewById(R.id.rd_user_row_tag).setVisibility(View.GONE);

            tblUsers.addView(row);
        }
    }
}
