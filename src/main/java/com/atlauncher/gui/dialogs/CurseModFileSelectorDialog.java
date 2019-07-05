/*
 * ATLauncher - https://github.com/ATLauncher/ATLauncher
 * Copyright (C) 2013-2019 ATLauncher
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.atlauncher.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;

import com.atlauncher.App;
import com.atlauncher.data.Instance;
import com.atlauncher.data.InstanceV2;
import com.atlauncher.data.Language;
import com.atlauncher.data.curse.CurseFile;
import com.atlauncher.data.curse.CurseFileDependency;
import com.atlauncher.data.curse.CurseMod;
import com.atlauncher.gui.card.CurseFileDependencyCard;
import com.atlauncher.managers.DialogManager;
import com.atlauncher.utils.CurseApi;
import com.atlauncher.utils.Utils;

public class CurseModFileSelectorDialog extends JDialog {
    private static final long serialVersionUID = -6984886874482721558L;
    private int filesLength = 0;
    private CurseMod mod;
    private Instance instance;
    private InstanceV2 instanceV2;

    private JPanel filesPanel;
    private JPanel dependenciesPanel = new JPanel(new FlowLayout());
    private JButton addButton;
    private JLabel versionsLabel;
    private JComboBox<CurseFile> filesDropdown;
    private List<CurseFile> files = new ArrayList<>();

    public CurseModFileSelectorDialog(CurseMod mod, Instance instance) {
        super(App.settings.getParent(), ModalityType.APPLICATION_MODAL);

        this.mod = mod;
        this.instance = instance;

        setupComponents();
    }

    public CurseModFileSelectorDialog(CurseMod mod, InstanceV2 instanceV2) {
        super(App.settings.getParent(), ModalityType.APPLICATION_MODAL);

        this.mod = mod;
        this.instanceV2 = instanceV2;

        setupComponents();
    }

    private void setupComponents() {
        setTitle(Language.INSTANCE.localize("common.installing") + " " + mod.name);

        setSize(500, 200);
        setLocationRelativeTo(App.settings.getParent());
        setLayout(new BorderLayout());
        setResizable(false);
        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        addButton = new JButton(Language.INSTANCE.localize("common.add"));
        addButton.setEnabled(false);

        dependenciesPanel.setVisible(false);
        dependenciesPanel.setBorder(BorderFactory.createTitledBorder("The below mods need to be installed"));

        // Top Panel Stuff
        JPanel top = new JPanel();
        top.add(new JLabel(Language.INSTANCE.localize("common.installing") + " " + mod.name));

        // Middle Panel Stuff
        JPanel middle = new JPanel(new BorderLayout());

        // Middle Panel Stuff
        filesPanel = new JPanel(new FlowLayout());
        filesPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

        versionsLabel = new JLabel(Language.INSTANCE.localize("instance.versiontoinstall") + ": ");
        filesPanel.add(versionsLabel);

        filesDropdown = new JComboBox<>();
        filesDropdown.setEnabled(false);
        filesPanel.add(filesDropdown);

        JScrollPane scrollPane = new JScrollPane(dependenciesPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setPreferredSize(new Dimension(550, 250));

        middle.add(filesPanel, BorderLayout.NORTH);
        middle.add(scrollPane, BorderLayout.SOUTH);

        this.getFiles();

        // Bottom Panel Stuff
        JPanel bottom = new JPanel();
        bottom.setLayout(new FlowLayout());

        addButton.addActionListener(e -> {
            CurseFile file = (CurseFile) filesDropdown.getSelectedItem();

            final JDialog dialog = new JDialog(this,
                    Language.INSTANCE.localize("common.installing") + " " + file.displayName,
                    ModalityType.DOCUMENT_MODAL);

            dialog.setLocationRelativeTo(this);
            dialog.setSize(300, 100);
            dialog.setResizable(false);

            JPanel topPanel = new JPanel();
            topPanel.setLayout(new BorderLayout());
            final JLabel doing = new JLabel(Language.INSTANCE.localize("common.installing") + " " + file.displayName);
            doing.setHorizontalAlignment(JLabel.CENTER);
            doing.setVerticalAlignment(JLabel.TOP);
            topPanel.add(doing);

            JPanel bottomPanel = new JPanel();
            bottomPanel.setLayout(new BorderLayout());

            JProgressBar progressBar = new JProgressBar(0, 100);
            bottomPanel.add(progressBar, BorderLayout.NORTH);
            progressBar.setIndeterminate(true);

            dialog.add(topPanel, BorderLayout.CENTER);
            dialog.add(bottomPanel, BorderLayout.SOUTH);

            Runnable r = () -> {
                if (this.instanceV2 != null) {
                    instanceV2.addFileFromCurse(mod, file);
                } else {
                    instance.addFileFromCurse(mod, file);
                }
                dialog.dispose();
                dispose();
            };

            new Thread(r).start();

            dialog.setVisible(true);
        });

        filesDropdown.addActionListener(e -> {
            CurseFile selectedFile = (CurseFile) filesDropdown.getSelectedItem();

            dependenciesPanel.setVisible(false);

            // this file has dependencies
            if (selectedFile.dependencies.size() != 0) {
                // check to see which required ones we don't already have
                List<CurseFileDependency> dependencies = selectedFile.dependencies.stream()
                        .filter(dependency -> dependency.isRequired() && instance.getInstalledMods().stream()
                                .filter(installedMod -> installedMod.isFromCurse()
                                        && installedMod.getCurseModId() == dependency.addonId)
                                .count() == 0)
                        .collect(Collectors.toList());

                if (dependencies.size() != 0) {
                    dependenciesPanel.removeAll();

                    if (this.instanceV2 != null) {
                        dependencies.forEach(dependency -> dependenciesPanel
                                .add(new CurseFileDependencyCard(selectedFile, dependency, instanceV2)));
                    } else {
                        dependencies.forEach(dependency -> dependenciesPanel
                                .add(new CurseFileDependencyCard(selectedFile, dependency, instance)));
                    }

                    dependenciesPanel.setLayout(new GridLayout(dependencies.size() < 2 ? 1 : dependencies.size() / 2,
                            (dependencies.size() / 2) + 1));

                    setSize(550, 400);
                    setLocationRelativeTo(App.settings.getParent());

                    dependenciesPanel.setVisible(true);

                    scrollPane.repaint();
                    scrollPane.validate();
                }
            }
        });

        JButton cancel = new JButton(Language.INSTANCE.localize("common.cancel"));
        cancel.addActionListener(e -> dispose());
        bottom.add(addButton);
        bottom.add(cancel);

        add(top, BorderLayout.NORTH);
        add(middle, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
        setVisible(true);
    }

    protected void getFiles() {
        versionsLabel.setVisible(true);
        filesDropdown.setVisible(true);

        Runnable r = () -> {
            files.addAll(CurseApi.getFilesForMod(mod.id).stream()
                    .sorted(Comparator.comparingInt((CurseFile file) -> file.id).reversed())
                    .filter(file -> file.gameVersion.contains(
                            this.instanceV2 != null ? this.instanceV2.id : this.instance.getMinecraftVersion()))
                    .collect(Collectors.toList()));

            // ensures that font width is taken into account
            for (CurseFile file : files) {
                filesLength = Math.max(filesLength,
                        getFontMetrics(Utils.getFont()).stringWidth(file.displayName) + 100);
            }

            // try to filter out non compatable mods
            files.stream().filter(version -> {
                String fileName = version.fileName.toLowerCase();
                String displayName = version.displayName.toLowerCase();

                if ((this.instanceV2 != null ? this.instanceV2.launcher.loaderVersion
                        : this.instance.getLoaderVersion()).isFabric()) {
                    return !displayName.contains("-forge-") && !displayName.contains("(forge)")
                            && !displayName.contains("[forge") && !fileName.contains("forgemod");
                }

                if (!(this.instanceV2 != null ? this.instanceV2.launcher.loaderVersion
                        : this.instance.getLoaderVersion()).isFabric()) {
                    return !displayName.toLowerCase().contains("-fabric-") && !displayName.contains("(fabric)")
                            && !displayName.contains("[fabric") && !fileName.contains("fabricmod");
                }

                return true;
            }).forEach(version -> {
                filesDropdown.addItem(version);
            });

            if (filesDropdown.getItemCount() == 0) {
                DialogManager.okDialog().setParent(CurseModFileSelectorDialog.this).setTitle("No files found")
                        .setContent("No files found for this mod").setType(DialogManager.ERROR).show();
                dispose();
            }

            // ensures that the dropdown is at least 200 px wide
            filesLength = Math.max(200, filesLength);

            // ensures that there is a maximum width of 350 px to prevent overflow
            filesLength = Math.min(350, filesLength);

            filesDropdown.setPreferredSize(new Dimension(filesLength, 25));

            filesDropdown.setEnabled(true);
            versionsLabel.setVisible(true);
            filesDropdown.setVisible(true);
            addButton.setEnabled(true);
            filesDropdown.setEnabled(true);
        };

        new Thread(r).start();
    }
}
