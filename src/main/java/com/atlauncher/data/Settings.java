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
package com.atlauncher.data;

import java.awt.Dialog.ModalityType;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.nio.file.Files;
import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;

import com.atlauncher.App;
import com.atlauncher.FileSystem;
import com.atlauncher.Gsons;
import com.atlauncher.LogManager;
import com.atlauncher.Update;
import com.atlauncher.data.minecraft.MojangStatus;
import com.atlauncher.exceptions.InvalidMinecraftVersion;
import com.atlauncher.exceptions.InvalidPack;
import com.atlauncher.gui.LauncherConsole;
import com.atlauncher.gui.components.LauncherBottomBar;
import com.atlauncher.gui.dialogs.ProgressDialog;
import com.atlauncher.gui.tabs.InstancesTab;
import com.atlauncher.gui.tabs.NewsTab;
import com.atlauncher.gui.tabs.PacksTab;
import com.atlauncher.managers.DialogManager;
import com.atlauncher.thread.LoggingThread;
import com.atlauncher.utils.ATLauncherAPIUtils;
import com.atlauncher.utils.FileUtils;
import com.atlauncher.utils.HTMLUtils;
import com.atlauncher.utils.Hashing;
import com.atlauncher.utils.Java;
import com.atlauncher.utils.MojangAPIUtils;
import com.atlauncher.utils.OS;
import com.atlauncher.utils.Timestamper;
import com.atlauncher.utils.Utils;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import net.arikia.dev.drpc.DiscordRPC;

/**
 * Settings class for storing all data for the Launcher and the settings of the
 * user.
 *
 * @author Ryan
 */
public class Settings {
    // Users Settings
    private String forgeLoggingLevel; // Logging level to use when running Minecraft with Forge
    private int initialMemory; // Initial RAM to use when launching Minecraft
    private int maximumMemory; // Maximum RAM to use when launching Minecraft
    private int permGen; // PermGenSize to use when launching Minecraft in MB
    private int windowWidth; // Width of the Minecraft window
    private int windowHeight; // Height of the Minecraft window
    private boolean maximiseMinecraft; // If Minecraft should start maximised
    private boolean saveCustomMods; // If custom mods should be saved between updates/reinstalls
    private boolean ignoreJavaOnInstanceLaunch; // If Java enforcement on instance launch should be
                                                // ignored
    private boolean usingCustomJavaPath; // If the user is using a custom java path
    private String javaPath; // Users path to Java
    private String javaParamaters; // Extra Java paramaters when launching Minecraft
    private boolean advancedBackup; // If advanced backup is enabled
    private boolean sortPacksAlphabetically; // If to sort packs default alphabetically
    private boolean keepLauncherOpen; // If we should close the Launcher after Minecraft has closed
    private boolean enableConsole; // If to show the console by default
    private boolean enableTrayIcon; // If to enable tray icon
    private boolean enableDiscordIntegration; // If to enable Discord integration
    private boolean enableLeaderboards; // If to enable the leaderboards
    private boolean enableLogs; // If to enable logs
    private boolean enableOpenEyeReporting; // If to enable OpenEye reporting
    private boolean enableProxy = false; // If proxy is in use
    private boolean enablePackTags = false;
    private String proxyHost; // The proxies host
    private int proxyPort; // The proxies port
    private String proxyType; // The type of proxy (socks, http)
    private int concurrentConnections; // Number of concurrent connections to open when downloading
    private int daysOfLogsToKeep; // Number of days of logs to keep
    private Account account; // Account using the Launcher
    private String addedPacks; // The Semi Public packs the user has added to the Launcher
    private Proxy proxy = null; // The proxy object if any
    private String theme; // The theme to use
    private String dateFormat; // The date format to use
    private boolean hideOldJavaWarning; // If the user has hidden the old Java warning
    private boolean hideJavaLetsEncryptWarning; // If the user has hidden the <= java 8.101 warning
    private boolean hideJava9Warning; // If the user has hidden the Java 8 warning
    private boolean enableServerChecker; // If to enable server checker
    private int serverCheckerWait; // Time to wait in minutes between checking server status
    // General backup settings
    private boolean autoBackup; // Whether backups are created on instance close
    private String lastSelectedSync; // The last service selected for syncing
    private boolean notifyBackup; // Whether to notify the user on successful backup or restore
    // Dropbox settings
    private String dropboxFolderLocation; // Location of dropbox if defined by user
    // Packs, Instances and Accounts
    private LauncherVersion latestLauncherVersion; // Latest Launcher version
    private List<DownloadableFile> launcherFiles; // Files the Launcher needs to download
    private List<News> news; // News
    private Map<String, MinecraftVersion> minecraftVersions; // Minecraft versions
    public List<Pack> packs; // Packs in the Launcher
    public List<Instance> instances = new ArrayList<>(); // Users Installed Instances
    public List<InstanceV2> instancesV2 = new ArrayList<>(); // Users Installed Instances (new format)
    private List<Account> accounts = new ArrayList<>(); // Accounts in the Launcher
    private List<MinecraftServer> checkingServers = new ArrayList<>();
    // Directories and Files for the Launcher
    private File baseDir, backupsDir, runtimesDir, configsDir, themesDir, jsonDir, versionsDir, imagesDir, skinsDir,
            toolsDir, jarsDir, commonConfigsDir, assetsDir, resourcesDir, librariesDir, gameLibrariesDir, loadersDir,
            languagesDir, downloadsDir, usersDownloadsFolder, instancesDir, serversDir, tempDir, failedDownloadsDir,
            instancesDataFile, checkingServersFile, userDataFile, propertiesFile, logsDir;
    // Launcher Settings
    private JFrame parent; // Parent JFrame of the actual Launcher
    private Properties properties = new Properties(); // Properties to store everything in
    private LauncherConsole console; // The Launcher's Console
    private InstancesTab instancesPanel; // The instances panel
    private NewsTab newsPanel; // The news panel
    private PacksTab vanillaPacksPanel; // The vanilla packs panel
    private PacksTab featuredPacksPanel; // The featured packs panel
    private PacksTab packsPanel; // The packs panel
    private LauncherBottomBar bottomBar; // The bottom bar
    private boolean hadPasswordDialog = false; // If the user has seen the password dialog
    private boolean firstTimeRun = false; // If this is the first time the Launcher has been run
    private boolean offlineMode = false; // If offline mode is enabled
    private Process minecraftProcess = null; // The process minecraft is running on
    private boolean minecraftLaunched = false; // If Minecraft has been Launched
    private String userAgent = "Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/537.36 (KHTML, " + ""
            + "like Gecko) Chrome/28.0.1500.72 Safari/537.36";
    private boolean minecraftLoginServerUp = false; // If the Minecraft Login server is up
    private boolean minecraftSessionServerUp = false; // If the Minecraft Session server is up
    @SuppressWarnings("unused")
    private DropboxSync dropbox;
    private Timer checkingServersTimer = null; // Timer used for checking servers

    public Settings() {
        setupFiles(); // Setup all the file and directory variables
        checkFolders(); // Checks the setup of the folders and makes sure they're there
        clearTempDir(); // Cleans all files in the Temp Dir
        loadStartingProperties(); // Get users Console preference and Java Path
    }

    public void loadConsole() {
        console = new LauncherConsole();
        LogManager.start();
    }

    public void setupFiles() {
        baseDir = FileSystem.getCoreGracefully().toFile();
        usersDownloadsFolder = new File(System.getProperty("user.home"), "Downloads");
        logsDir = new File(baseDir, "Logs");
        backupsDir = new File(baseDir, "Backups");
        runtimesDir = new File(baseDir, "runtimes");
        configsDir = new File(baseDir, "Configs");
        themesDir = new File(configsDir, "Themes");
        jsonDir = new File(configsDir, "JSON");
        versionsDir = new File(configsDir, "Versions");
        imagesDir = new File(configsDir, "Images");
        skinsDir = new File(imagesDir, "Skins");
        toolsDir = new File(configsDir, "Tools");
        jarsDir = new File(configsDir, "Jars");
        commonConfigsDir = new File(configsDir, "Common");
        resourcesDir = new File(configsDir, "Resources");
        assetsDir = new File(baseDir, "assets");
        librariesDir = new File(configsDir, "Libraries");
        gameLibrariesDir = new File(baseDir, "libraries");
        loadersDir = new File(baseDir, "loaders");
        languagesDir = new File(configsDir, "Languages");
        downloadsDir = new File(baseDir, "Downloads");
        instancesDir = new File(baseDir, "Instances");
        serversDir = new File(baseDir, "Servers");
        tempDir = new File(baseDir, "Temp");
        failedDownloadsDir = new File(baseDir, "FailedDownloads");
        instancesDataFile = new File(configsDir, "instancesdata");
        checkingServersFile = new File(configsDir, "checkingservers.json");
        userDataFile = new File(configsDir, "userdata");
        propertiesFile = new File(configsDir, Constants.LAUNCHER_NAME + ".conf");
    }

    public void loadEverything() {
        if (App.forceOfflineMode) {
            this.offlineMode = true;
        }

        if (hasUpdatedFiles()) {
            downloadUpdatedFiles(); // Downloads updated files on the server
        }

        checkForLauncherUpdate();

        addExecutableBitToTools();

        loadNews(); // Load the news

        loadMinecraftVersions(); // Load info about the different Minecraft versions

        loadPacks(); // Load the Packs available in the Launcher

        loadUsers(); // Load the Testers and Allowed Players for the packs

        loadInstances(); // Load the users installed Instances

        loadAccounts(); // Load the saved Accounts

        loadCheckingServers(); // Load the saved servers we're checking with the tool

        loadProperties(); // Load the users Properties

        if (this.isUsingCustomJavaPath()) {
            checkForValidJavaPath(true); // Checks for a valid Java path
        }

        console.setupLanguage(); // Setup language on the console

        clearOldLogs(); // Clear all the old logs out

        checkResources(); // Check for new format of resources

        checkAccountUUIDs(); // Check for accounts UUID's and add them if necessary

        changeInstanceUserLocks(); // Changes any instances user locks to UUIDs if available

        checkAccountsForNameChanges(); // Check account for username changes

        if (OS.isWindows() && !OS.is64Bit() && OS.isWindows64Bit()) {
            LogManager.warn("You're using 32 bit Java on a 64 bit Windows install!");

            int ret = DialogManager.yesNoDialog().setTitle(Language.INSTANCE.localize("settings.running32bittitle"))
                    .setContent(HTMLUtils.centerParagraph(
                            Language.INSTANCE.localizeWithReplace("settings.running32bit", "<br/><br/>")))
                    .setType(DialogManager.ERROR).show();

            if (ret == 0) {
                OS.openWebBrowser("http://www.atlauncher.com/help/32bit/");
                System.exit(0);
            }
        }

        if (Java.isMinecraftJavaNewerThanJava8() && !this.hideJava9Warning) {
            LogManager.warn("You're using a newer version of Java than Java 8! Modpacks may not launch!");

            int ret = DialogManager.optionDialog().setTitle(Language.INSTANCE.localize("settings.java9warningtitle"))
                    .setContent(HTMLUtils.centerParagraph(
                            Language.INSTANCE.localizeWithReplace("settings.java9warning", "<br/><br/>")))
                    .addOption(Language.INSTANCE.localize("common.download"), true)
                    .addOption(Language.INSTANCE.localize("common.ok"))
                    .addOption(Language.INSTANCE.localize("instance.dontremindmeagain")).setType(DialogManager.WARNING)
                    .show();

            if (ret == 0) {
                OS.openWebBrowser("http://atl.pw/java8download");
                System.exit(0);
            } else if (ret == 2) {
                this.hideJava9Warning = true;
                this.saveProperties();
            }
        }

        if (!Java.isUsingJavaSupportingLetsEncrypt() && !this.hideJavaLetsEncryptWarning) {
            LogManager.warn("You're using an old version of Java that may not work!");

            int ret = DialogManager.optionDialog().setTitle(Language.INSTANCE.localize("settings.unsupportedjavatitle"))
                    .setContent(HTMLUtils.centerParagraph(
                            Language.INSTANCE.localizeWithReplace("settings.unsupportedjavaletsencrypt", "<br/><br/>")))
                    .addOption(Language.INSTANCE.localize("common.download"), true)
                    .addOption(Language.INSTANCE.localize("common.ok"))
                    .addOption(Language.INSTANCE.localize("instance.dontremindmeagain")).setType(DialogManager.ERROR)
                    .show();

            if (ret == 0) {
                OS.openWebBrowser("http://atl.pw/java8download");
                System.exit(0);
            } else if (ret == 2) {
                this.hideJavaLetsEncryptWarning = true;
                this.saveProperties();
            }
        }

        if (!Java.isJava7OrAbove(true) && !this.hideOldJavaWarning) {
            LogManager.warn("You're using an old unsupported version of Java (Java 7 or older)!");

            int ret = DialogManager.optionDialog().setTitle(Language.INSTANCE.localize("settings.unsupportedjavatitle"))
                    .setContent(HTMLUtils.centerParagraph(
                            Language.INSTANCE.localizeWithReplace("settings.unsupportedjava", "<br/><br/>")))
                    .addOption(Language.INSTANCE.localize("common.download"), true)
                    .addOption(Language.INSTANCE.localize("common.ok"))
                    .addOption(Language.INSTANCE.localize("instance.dontremindmeagain")).setType(DialogManager.WARNING)
                    .show();

            if (ret == 0) {
                OS.openWebBrowser("http://atl.pw/java8download");
                System.exit(0);
            } else if (ret == 2) {
                this.hideOldJavaWarning = true;
                this.saveProperties();
            }
        }

        if (this.advancedBackup) {
            dropbox = new DropboxSync();
        }

        if (!this.hadPasswordDialog) {
            checkAccounts(); // Check accounts with stored passwords
        }

        if (this.enableServerChecker) {
            this.startCheckingServers();
        }

        if (this.enableLogs) {
            App.TASKPOOL.execute(ATLauncherAPIUtils::postSystemInfo);
        }
    }

    private void checkAccountsForNameChanges() {
        LogManager.info("Checking For Username Changes");

        boolean somethingChanged = false;

        for (Account account : this.accounts) {
            if (account.checkForUsernameChange()) {
                somethingChanged = true;
            }
        }

        if (somethingChanged) {
            this.saveAccounts();
        }

        LogManager.info("Checking For Username Changes Complete");
    }

    public void checkForValidJavaPath(boolean save) {
        File java = new File(App.settings.getJavaPath(),
                "bin" + File.separator + "java" + (OS.isWindows() ? ".exe" : ""));

        if (!java.exists()) {
            LogManager.error("Custom Java Path Is Incorrect! Defaulting to valid value!");
            this.setJavaPath(OS.getDefaultJavaPath());

            if (save) {
                this.saveProperties();
            }
        }
    }

    public void startCheckingServers() {
        if (this.checkingServersTimer != null) {
            // If it's not null, cancel and purge tasks left
            this.checkingServersTimer.cancel();
            this.checkingServersTimer.purge(); // not sure if needed or not
        }

        if (this.enableServerChecker) {
            this.checkingServersTimer = new Timer();
            this.checkingServersTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    for (MinecraftServer server : checkingServers) {
                        server.checkServer();
                    }
                }
            }, 0, this.getServerCheckerWaitInMilliseconds());
        }
    }

    public void checkAccounts() {
        boolean matches = false;
        if (this.accounts != null || this.accounts.size() >= 1) {
            for (Account account : this.accounts) {
                if (account.isRemembered()) {
                    matches = true;
                }
            }
        }
        if (matches) {
            int ret = DialogManager.optionDialog().setTitle(Language.INSTANCE.localize("account.securitywarningtitle"))
                    .setContent(HTMLUtils
                            .centerParagraph(Language.INSTANCE.localizeWithReplace("account.securitywarning", "<br/>")))
                    .addOption(Language.INSTANCE.localize("common.ok"), true)
                    .addOption(Language.INSTANCE.localize("account.removepasswords")).setType(DialogManager.WARNING)
                    .show();

            if (ret == 1) {
                for (Account account : this.accounts) {
                    if (account.isRemembered()) {
                        account.setRemember(false);
                    }
                }
                this.saveAccounts();
            }
        }
        this.saveProperties();
    }

    public void clearOldLogs() {
        LogManager.debug("Clearing out old logs");

        File logFile1 = new File(getBaseDir(), Constants.LAUNCHER_NAME + "-Log-1.txt");
        File logFile2 = new File(getBaseDir(), Constants.LAUNCHER_NAME + "-Log-2.txt");
        File logFile3 = new File(getBaseDir(), Constants.LAUNCHER_NAME + "-Log-3.txt");

        if (logFile3.exists()) {
            Utils.delete(logFile3);
        }

        if (logFile2.exists()) {
            Utils.delete(logFile2);
        }

        if (logFile1.exists()) {
            Utils.delete(logFile1);
        }

        Date toDeleteAfter = new Date();

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(toDeleteAfter);
        calendar.add(Calendar.DATE, -(getDaysOfLogsToKeep()));
        toDeleteAfter = calendar.getTime();

        for (File file : this.logsDir.listFiles(Utils.getLogsFileFilter())) {
            try {
                Date date = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")
                        .parse(file.getName().replace(Constants.LAUNCHER_NAME + "-Log_", "").replace(".log", ""));

                if (date.before(toDeleteAfter)) {
                    Utils.delete(file);
                    LogManager.debug("Deleting log file " + file.getName());
                }
            } catch (ParseException e) {
                LogManager.error("Invalid log file " + file.getName());
            }
        }

        LogManager.debug("Finished clearing out old logs");
    }

    public void clearAllLogs() {
        File logFile1 = new File(getBaseDir(), Constants.LAUNCHER_NAME + "-Log-1.txt");
        File logFile2 = new File(getBaseDir(), Constants.LAUNCHER_NAME + "-Log-2.txt");
        File logFile3 = new File(getBaseDir(), Constants.LAUNCHER_NAME + "-Log-3.txt");

        if (logFile3.exists()) {
            Utils.delete(logFile3);
        }

        if (logFile2.exists()) {
            Utils.delete(logFile2);
        }

        if (logFile1.exists()) {
            Utils.delete(logFile1);
        }

        for (File file : this.logsDir.listFiles(Utils.getLogsFileFilter())) {
            if (file.getName().equals(LoggingThread.filename)) {
                continue; // Skip current log
            }

            Utils.delete(file);
        }
    }

    public void checkResources() {
        LogManager.debug("Checking if using old format of resources");
        // moving from Resources to more vanilla 'assets' folder
        if (this.resourcesDir.exists() && this.resourcesDir.isDirectory()) {
            final ProgressDialog dialog = new ProgressDialog(
                    Language.INSTANCE.localize("settings.rearrangingresources"), 0,
                    Language.INSTANCE.localize("settings.rearrangingresources"), null);
            Thread thread = new Thread(() -> {
                Utils.moveDirectory(new File(getResourcesDir(), "indexes"), new File(assetsDir, "indexes"));
                Utils.moveDirectory(new File(getResourcesDir(), "objects"), new File(assetsDir, "objects"));
                Utils.moveDirectory(new File(getResourcesDir(), "virtual"), new File(assetsDir, "virtual"));

                Utils.delete(getResourcesDir());

                dialog.close();
            });
            dialog.addThread(thread);
            dialog.start();
        }
        LogManager.debug("Finished checking if using old format of resources");
    }

    public void checkAccountUUIDs() {
        LogManager.debug("Checking account UUID's");
        LogManager.info("Checking account UUID's!");
        for (Account account : this.accounts) {
            if (account.isUUIDNull()) {
                account.setUUID(MojangAPIUtils.getUUID(account.getMinecraftUsername()));
                this.saveAccounts();
            }
        }
        LogManager.debug("Finished checking account UUID's");
    }

    public void changeInstanceUserLocks() {
        LogManager.debug("Changing instances user locks to UUID's");

        boolean wereChanges = false;

        for (Instance instance : this.instances) {
            if (instance.getInstalledBy() != null) {
                boolean found = false;

                for (Account account : this.accounts) {
                    // This is the user who installed this so switch to their UUID
                    if (account.getMinecraftUsername().equalsIgnoreCase(instance.getInstalledBy())) {
                        found = true;
                        wereChanges = true;

                        instance.removeInstalledBy();

                        // If the accounts UUID is null for whatever reason, don't set the lock
                        if (!account.isUUIDNull()) {
                            instance.setUserLock(account.getUUIDNoDashes());
                        }
                        break;
                    }
                }

                // If there were no accounts with that username, we remove the lock and old
                // installed by
                if (!found) {
                    wereChanges = true;

                    instance.removeInstalledBy();
                    instance.removeUserLock();
                }
            }
        }

        if (wereChanges) {
            this.saveAccounts();
            this.saveInstances();
        }

        LogManager.debug("Finished changing instances user locks to UUID's");
    }

    public void checkMojangStatus() {
        try {
            MojangStatus status = com.atlauncher.network.Download.build().setUrl("https://status.mojang.com/check")
                    .asClass(MojangStatus.class);
            minecraftLoginServerUp = status.isAuthServerUp();
            minecraftSessionServerUp = status.isSessionServerUp();
        } catch (Exception e) {
            LogManager.logStackTrace(e);
            minecraftSessionServerUp = false;
            minecraftLoginServerUp = false;
        }
    }

    public Status getMojangStatus() {
        if (minecraftLoginServerUp && minecraftSessionServerUp) {
            return Status.ONLINE;
        } else if (!minecraftLoginServerUp && !minecraftSessionServerUp) {
            return Status.OFFLINE;
        } else {
            return Status.PARTIAL;
        }
    }

    public boolean launcherHasUpdate() {
        try {
            this.latestLauncherVersion = Gsons.DEFAULT.fromJson(new FileReader(new File(this.jsonDir, "version.json")),
                    LauncherVersion.class);
        } catch (JsonSyntaxException | FileNotFoundException | JsonIOException e) {
            LogManager.logStackTrace("Exception when loading latest launcher version!", e);
        }

        return this.latestLauncherVersion != null && Constants.VERSION.needsUpdate(this.latestLauncherVersion);
    }

    public void downloadUpdate() {
        try {
            File thisFile = new File(Update.class.getProtectionDomain().getCodeSource().getLocation().getPath());
            String path = thisFile.getCanonicalPath();
            path = URLDecoder.decode(path, "UTF-8");
            String toget;
            String saveAs = thisFile.getName();
            if (path.contains(".exe")) {
                toget = "exe";
            } else {
                toget = "jar";
            }
            File newFile = new File(getTempDir(), saveAs);
            LogManager.info("Downloading Launcher Update");

            com.atlauncher.network.Download.build()
                    .setUrl(String.format("%s/%s.%s", Constants.DOWNLOAD_SERVER, Constants.LAUNCHER_NAME, toget))
                    .downloadTo(newFile.toPath()).downloadFile();

            runUpdate(path, newFile.getAbsolutePath());
        } catch (IOException e) {
            LogManager.logStackTrace(e);
        }
    }

    public void runUpdate(String currentPath, String temporaryUpdatePath) {
        List<String> arguments = new ArrayList<>();

        String path = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        if (OS.isWindows()) {
            path += "w";
        }
        arguments.add(path);
        arguments.add("-cp");
        arguments.add(temporaryUpdatePath);
        arguments.add("com.atlauncher.Update");
        arguments.add(currentPath);
        arguments.add(temporaryUpdatePath);

        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(arguments);

        LogManager.info("Running launcher update with command " + arguments);

        try {
            processBuilder.start();
        } catch (IOException e) {
            LogManager.logStackTrace(e);
        }

        System.exit(0);
    }

    private void getFileHashes() {
        this.launcherFiles = null;

        java.lang.reflect.Type type = new TypeToken<List<DownloadableFile>>() {
        }.getType();

        try {
            this.launcherFiles = com.atlauncher.network.Download.build()
                    .setUrl(String.format("%s/launcher/json/hashes.json", Constants.DOWNLOAD_SERVER)).asType(type);
        } catch (Exception e) {
            LogManager.logStackTrace("Error loading in file hashes!", e);
        }
    }

    /**
     * This checks the servers hashes.json file and gets the files that the Launcher
     * needs to have
     */
    private List<com.atlauncher.network.Download> getLauncherFiles() {
        getFileHashes(); // Get File Hashes
        if (this.launcherFiles == null) {
            this.offlineMode = true;
            return null;
        }
        ArrayList<com.atlauncher.network.Download> downloads = new ArrayList<>();
        for (DownloadableFile file : this.launcherFiles) {
            if (file.isLauncher()) {
                continue;
            }
            downloads.add(file.getDownload());
        }
        return downloads;
    }

    public void downloadUpdatedFiles() {
        List<com.atlauncher.network.Download> downloads = getLauncherFiles();
        if (downloads != null) {
            ExecutorService executor = Executors.newFixedThreadPool(this.concurrentConnections);
            for (final com.atlauncher.network.Download download : downloads) {
                executor.execute(() -> {
                    if (download.needToDownload()) {
                        LogManager.info("Downloading Launcher File " + download.to.getFileName());
                        try {
                            download.downloadFile();
                        } catch (IOException e) {
                            LogManager.logStackTrace(e);
                        }
                    }
                });
            }
            executor.shutdown();
            while (!executor.isTerminated()) {
            }
        }

        LogManager.info("Finished downloading updated files!");

        if (Language.INSTANCE.getCurrent() != null) {
            try {
                Language.INSTANCE.reload(Language.INSTANCE.getCurrent());
            } catch (IOException e) {
                LogManager.logStackTrace("Couldn't reload langauge " + Language.INSTANCE.getCurrent(), e);
            }
        }
    }

    /**
     * This checks the servers hashes.json file and looks for new/updated files that
     * differ from what the user has
     */
    public boolean hasUpdatedFiles() {
        if (isInOfflineMode()) {
            return false;
        }
        LogManager.info("Checking for updated files!");
        List<com.atlauncher.network.Download> downloads = getLauncherFiles();
        if (downloads == null) {
            this.offlineMode = true;
            return false;
        }
        for (com.atlauncher.network.Download download : downloads) {
            if (download.needToDownload()) {
                LogManager.info("Updates found!");
                return true; // 1 file needs to be updated so there is updated files
            }
        }
        LogManager.info("No updates found!");
        return false; // No updates
    }

    public void reloadLauncherData() {
        final JDialog dialog = new JDialog(this.parent, ModalityType.APPLICATION_MODAL);
        dialog.setSize(300, 100);
        dialog.setTitle("Updating Launcher");
        dialog.setLocationRelativeTo(App.settings.getParent());
        dialog.setLayout(new FlowLayout());
        dialog.setResizable(false);
        dialog.add(new JLabel("Updating Launcher... Please Wait"));
        App.TASKPOOL.execute(() -> {
            if (hasUpdatedFiles()) {
                downloadUpdatedFiles(); // Downloads updated files on the server
            }
            checkForLauncherUpdate();
            addExecutableBitToTools();
            loadNews(); // Load the news
            reloadNewsPanel(); // Reload news panel
            loadPacks(); // Load the Packs available in the Launcher
            reloadVanillaPacksPanel(); // Reload packs panel
            reloadFeaturedPacksPanel(); // Reload packs panel
            reloadPacksPanel(); // Reload packs panel
            loadUsers(); // Load the Testers and Allowed Players for the packs
            loadInstances(); // Load the users installed Instances
            reloadInstancesPanel(); // Reload instances panel
            dialog.setVisible(false); // Remove the dialog
            dialog.dispose(); // Dispose the dialog
        });
        dialog.setVisible(true);
    }

    private void checkForLauncherUpdate() {
        if (App.noLauncherUpdate) {
            return;
        }

        LogManager.debug("Checking for launcher update");
        if (launcherHasUpdate()) {
            if (!App.wasUpdated) {
                downloadUpdate(); // Update the Launcher
            } else {
                DialogManager.okDialog().setTitle("Update Failed!")
                        .setContent(HTMLUtils.centerParagraph("Update failed. Please click Ok to close "
                                + "the launcher and open up the downloads page.<br/><br/>Download "
                                + "the update and replace the old " + Constants.LAUNCHER_NAME + " file."))
                        .setType(DialogManager.ERROR).show();
                OS.openWebBrowser("https://www.atlauncher.com/downloads/");
                System.exit(0);
            }
        }
        LogManager.debug("Finished checking for launcher update");
    }

    private void addExecutableBitToTools() {
        File[] files = toolsDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (!file.canExecute()) {
                    LogManager.info("Executable bit being set on " + file.getName());
                    file.setExecutable(true);
                }
            }
        }
    }

    /**
     * Checks the directory to make sure all the necessary folders are there
     */
    private void checkFolders() {
        File[] files = { backupsDir, runtimesDir, configsDir, themesDir, jsonDir, commonConfigsDir, imagesDir, skinsDir,
                toolsDir, jarsDir, assetsDir, this.getObjectsAssetsDir(), this.getVirtualAssetsDir(),
                this.getIndexesAssetsDir(), librariesDir, gameLibrariesDir, loadersDir, languagesDir, downloadsDir,
                instancesDir, serversDir, tempDir, failedDownloadsDir, logsDir };
        for (File file : files) {
            if (!file.exists()) {
                file.mkdir();
            }
            if (!file.isDirectory()) {
                if (file.delete()) {
                    file.mkdir();
                }

            }
        }
    }

    /**
     * Returns the base directory
     *
     * @return File object for the base directory
     */
    public File getBaseDir() {
        return this.baseDir;
    }

    /**
     * Returns the backups directory
     *
     * @return File object for the backups directory
     */
    public File getBackupsDir() {
        return this.backupsDir;
    }

    public File getRuntimesDir() {
        return this.runtimesDir;
    }

    /**
     * Returns the configs directory
     *
     * @return File object for the configs directory
     */
    public File getConfigsDir() {
        return this.configsDir;
    }

    /**
     * Returns the themes directory
     *
     * @return File object for the themes directory
     */
    public File getThemesDir() {
        return this.themesDir;
    }

    /**
     * Returns the JSON directory
     *
     * @return File object for the JSON directory
     */
    public File getJSONDir() {
        return this.jsonDir;
    }

    /**
     * Returns the Versions directory
     *
     * @return File object for the Versions directory
     */
    public File getVersionsDir() {
        return this.versionsDir;
    }

    /**
     * Returns the common configs directory
     *
     * @return File object for the common configs directory
     */
    public File getCommonConfigsDir() {
        return this.commonConfigsDir;
    }

    /**
     * Returns the images directory
     *
     * @return File object for the images directory
     */
    public File getImagesDir() {
        return this.imagesDir;
    }

    /**
     * Returns the skins directory
     *
     * @return File object for the skins directory
     */
    public File getSkinsDir() {
        return this.skinsDir;
    }

    /**
     * Returns the tools directory
     *
     * @return File object for the tools directory
     */
    public File getToolsDir() {
        return this.toolsDir;
    }

    /**
     * Returns the jars directory
     *
     * @return File object for the jars directory
     */
    public File getJarsDir() {
        return this.jarsDir;
    }

    /**
     * Returns the resources directory
     *
     * @return File object for the resources directory
     */
    public File getResourcesDir() {
        return this.resourcesDir;
    }

    /**
     * Returns the assets directory
     *
     * @return File object for the resources directory
     */
    public File getAssetsDir() {
        return this.assetsDir;
    }

    public File getVirtualAssetsDir() {
        return new File(this.assetsDir, "virtual");
    }

    public File getObjectsAssetsDir() {
        return new File(this.assetsDir, "objects");
    }

    public File getIndexesAssetsDir() {
        return new File(this.assetsDir, "indexes");
    }

    public File getLegacyVirtualAssetsDir() {
        return new File(getVirtualAssetsDir(), "legacy");
    }

    public File getLogConfigsDir() {
        return new File(this.assetsDir, "log_configs");
    }

    /**
     * Returns the libraries directory
     *
     * @return File object for the libraries directory
     */
    public File getLibrariesDir() {
        return this.librariesDir;
    }

    /**
     * Returns the libraries directory for Minecraft
     *
     * @return File object for the libraries directory for Minecraft
     */
    public File getGameLibrariesDir() {
        return this.gameLibrariesDir;
    }

    /**
     * Returns the loaders directory
     *
     * @return File object for the loaders directory
     */
    public File getLoadersDir() {
        return this.loadersDir;
    }

    /**
     * Returns the languages directory
     *
     * @return File object for the languages directory
     */
    public File getLanguagesDir() {
        return this.languagesDir;
    }

    /**
     * Returns the downloads directory
     *
     * @return File object for the downloads directory
     */
    public File getDownloadsDir() {
        return this.downloadsDir;
    }

    /**
     * Returns the downloads directory for the user
     *
     * @return File object for the downloads directory for the users account
     */
    public File getUsersDownloadsDir() {
        return this.usersDownloadsFolder;
    }

    /**
     * Returns the instances directory
     *
     * @return File object for the instances directory
     */
    public File getInstancesDir() {
        return this.instancesDir;
    }

    /**
     * Returns the servers directory
     *
     * @return File object for the servers directory
     */
    public File getServersDir() {
        return this.serversDir;
    }

    /**
     * Returns the temp directory
     *
     * @return File object for the temp directory
     */
    public File getTempDir() {
        return this.tempDir;
    }

    public File getFailedDownloadsDir() {
        return this.failedDownloadsDir;
    }

    /**
     * Returns the logs directory
     *
     * @return File object for the logs directory
     */
    public File getLogsDir() {
        return this.logsDir;
    }

    /**
     * Deletes all files in the Temp directory
     */
    public void clearTempDir() {
        Utils.deleteContents(getTempDir());
    }

    /**
     * Returns the instancesdata file
     *
     * @return File object for the instancesdata file
     */
    public File getInstancesDataFile() {
        return instancesDataFile;
    }

    /**
     * Returns the checkingservers file
     *
     * @return File object for the checkingservers file
     */
    public File getCheckingServersFile() {
        return checkingServersFile;
    }

    /**
     * Sets the main parent JFrame reference for the Launcher
     *
     * @param parent The Launcher main JFrame
     */
    public void setParentFrame(JFrame parent) {
        this.parent = parent;
    }

    /**
     * Load the users Console preference from file
     */
    public void loadStartingProperties() {
        try {
            if (!propertiesFile.exists()) {
                propertiesFile.createNewFile();
            }
        } catch (IOException e) {
            DialogManager.okDialog().setTitle("Error!")
                    .setContent(HTMLUtils.centerParagraph("Cannot create the config file"
                            + ".<br/><br/>Make sure you're running the Launcher from somewhere with<br/>write"
                            + " permissions for your user account such as your Home/Users folder or desktop."))
                    .setType(DialogManager.ERROR).show();
            System.exit(0);
        }
        try {
            this.properties.load(new FileInputStream(propertiesFile));
            this.theme = properties.getProperty("theme", Constants.LAUNCHER_NAME);
            this.dateFormat = properties.getProperty("dateformat", "dd/M/yyy");
            if (!this.dateFormat.equalsIgnoreCase("dd/M/yyy") && !this.dateFormat.equalsIgnoreCase("M/dd/yyy")
                    && !this.dateFormat.equalsIgnoreCase("yyy/M/dd")) {
                this.dateFormat = "dd/M/yyy";
            }
            this.enablePackTags = Boolean.parseBoolean(properties.getProperty("enablepacktags", "false"));
            this.enableConsole = Boolean.parseBoolean(properties.getProperty("enableconsole", "true"));
            this.enableTrayIcon = Boolean.parseBoolean(properties.getProperty("enabletrayicon", "true"));
            this.enableDiscordIntegration = Boolean
                    .parseBoolean(properties.getProperty("enablediscordintegration", "true"));
            if (!properties.containsKey("usingcustomjavapath")) {
                this.usingCustomJavaPath = false;
                this.javaPath = OS.getDefaultJavaPath();
            } else {
                this.usingCustomJavaPath = Boolean.parseBoolean(properties.getProperty("usingcustomjavapath", "false"));
                if (isUsingCustomJavaPath()) {
                    this.javaPath = properties.getProperty("javapath", OS.getDefaultJavaPath());
                } else {
                    this.javaPath = OS.getDefaultJavaPath();
                }
            }

            this.enableProxy = Boolean.parseBoolean(properties.getProperty("enableproxy", "false"));

            if (this.enableProxy) {
                this.proxyHost = properties.getProperty("proxyhost", null);

                this.proxyPort = Integer.parseInt(properties.getProperty("proxyport", "0"));
                if (this.proxyPort <= 0 || this.proxyPort > 65535) {
                    this.enableProxy = false;
                }

                this.proxyType = properties.getProperty("proxytype", "");
                if (!this.proxyType.equals("SOCKS") && !this.proxyType.equals("HTTP")
                        && !this.proxyType.equals("DIRECT")) {
                    this.enableProxy = false;
                }
            } else {
                this.proxyHost = "";
                this.proxyPort = 0;
                this.proxyType = "";
            }

            this.concurrentConnections = Integer.parseInt(properties.getProperty("concurrentconnections", "8"));
            if (this.concurrentConnections < 1) {
                this.concurrentConnections = 8;
            }

            this.daysOfLogsToKeep = Integer.parseInt(properties.getProperty("daysoflogstokeep", "7"));
            if (this.daysOfLogsToKeep < 1 || this.daysOfLogsToKeep > 30) {
                this.daysOfLogsToKeep = 7;
            }
        } catch (IOException e) {
            LogManager.logStackTrace(e);
        }
    }

    public boolean enabledPackTags() {
        return this.enablePackTags;
    }

    public void setPackTags(boolean b) {
        this.enablePackTags = b;
    }

    /**
     * Load the properties from file
     */
    public void loadProperties() {
        LogManager.debug("Loading properties");
        try {
            this.properties.load(new FileInputStream(propertiesFile));
            this.firstTimeRun = Boolean.parseBoolean(properties.getProperty("firsttimerun", "true"));

            this.hadPasswordDialog = Boolean.parseBoolean(properties.getProperty("hadpassworddialog", "false"));

            this.hideOldJavaWarning = Boolean.parseBoolean(properties.getProperty("hideoldjavawarning", "false"));

            this.hideJavaLetsEncryptWarning = Boolean
                    .parseBoolean(properties.getProperty("hidejavaletsencryptwarning", "false"));

            this.hideJava9Warning = Boolean.parseBoolean(properties.getProperty("hideJava9Warning", "false"));

            String lang = properties.getProperty("language", "English");
            if (!isLanguageByName(lang)) {
                LogManager.warn("Invalid language " + lang + ". Defaulting to English!");
                lang = "English";
            }

            Language.INSTANCE.load(lang);

            this.forgeLoggingLevel = properties.getProperty("forgelogginglevel", "INFO");
            if (!this.forgeLoggingLevel.equalsIgnoreCase("SEVERE")
                    && !this.forgeLoggingLevel.equalsIgnoreCase("WARNING")
                    && !this.forgeLoggingLevel.equalsIgnoreCase("INFO")
                    && !this.forgeLoggingLevel.equalsIgnoreCase("CONFIG")
                    && !this.forgeLoggingLevel.equalsIgnoreCase("FINE")
                    && !this.forgeLoggingLevel.equalsIgnoreCase("FINER")
                    && !this.forgeLoggingLevel.equalsIgnoreCase("FINEST")) {
                LogManager.warn("Invalid Forge Logging level " + this.forgeLoggingLevel + ". Defaulting to INFO!");
                this.forgeLoggingLevel = "INFO";
            }

            if (OS.is64Bit()) {
                this.maximumMemory = Integer.parseInt(properties.getProperty("ram", "4096"));
                if (OS.getMaximumRam() != 0 && this.maximumMemory > OS.getMaximumRam()) {
                    LogManager.warn("Tried to allocate " + this.maximumMemory + "MB of Ram but only "
                            + OS.getMaximumRam() + "MB is available to use!");
                    int halfRam = (OS.getMaximumRam() / 1000) * 512;
                    int defaultRam = (halfRam >= 8192 ? 8192 : halfRam); // Default ram
                    this.maximumMemory = defaultRam; // User tried to allocate too much ram, set it
                    // back to half, capped at 8GB
                }
            } else {
                this.maximumMemory = Integer.parseInt(properties.getProperty("ram", "1024"));
                if (OS.getMaximumRam() != 0 && this.maximumMemory > OS.getMaximumRam()) {
                    LogManager.warn("Tried to allocate " + this.maximumMemory + "MB of Maximum Ram but only "
                            + OS.getMaximumRam() + "MB is available to use!");
                    this.maximumMemory = 1024; // User tried to allocate too much ram, set it back
                    // to 1GB
                }
            }

            this.initialMemory = Integer.parseInt(properties.getProperty("initialmemory", "512"));
            if (OS.getMaximumRam() != 0 && this.initialMemory > OS.getMaximumRam()) {
                LogManager.warn("Tried to allocate " + this.initialMemory + "MB of Initial Ram but only "
                        + OS.getMaximumRam() + "MB is available to use!");
                this.initialMemory = 512; // User tried to allocate too much ram, set it back to
                // 512MB
            } else if (this.initialMemory > this.maximumMemory) {
                LogManager.warn("Tried to allocate " + this.initialMemory + "MB of Initial Ram but maximum ram is "
                        + this.maximumMemory + "MB which is less!");
                this.initialMemory = 512; // User tried to allocate too much ram, set it back to
                                          // 512MB
            }

            // Default PermGen to 256 for 64 bit systems and 128 for 32 bit systems
            this.permGen = Integer.parseInt(properties.getProperty("permGen", (OS.is64Bit() ? "256" : "128")));

            this.windowWidth = Integer.parseInt(properties.getProperty("windowwidth", "854"));
            if (this.windowWidth > OS.getMaximumWindowWidth()) {
                LogManager.warn("Tried to set window width to " + this.windowWidth + " pixels but the maximum is "
                        + OS.getMaximumWindowWidth() + " pixels!");
                this.windowWidth = OS.getMaximumWindowWidth(); // User tried to make screen size
                // wider than they have
            }

            this.windowHeight = Integer.parseInt(properties.getProperty("windowheight", "480"));
            if (this.windowHeight > OS.getMaximumWindowHeight()) {
                LogManager.warn("Tried to set window height to " + this.windowHeight + " pixels but the maximum is "
                        + OS.getMaximumWindowHeight() + " pixels!");
                this.windowHeight = OS.getMaximumWindowHeight(); // User tried to make screen
                // size wider than they have
            }

            this.usingCustomJavaPath = Boolean.parseBoolean(properties.getProperty("usingcustomjavapath", "false"));

            if (isUsingCustomJavaPath()) {
                this.javaPath = properties.getProperty("javapath", OS.getDefaultJavaPath());
            } else {
                this.javaPath = OS.getDefaultJavaPath();
                if (OS.isUsingMacApp()) {
                    File oracleJava = new File(
                            "/Library/Internet Plug-Ins/JavaAppletPlugin.plugin/Contents/Home/bin/java");
                    if (oracleJava.exists() && oracleJava.canExecute()) {
                        this.setJavaPath("/Library/Internet Plug-Ins/JavaAppletPlugin.plugin/Contents/Home");
                    }
                }
            }

            this.javaParamaters = properties.getProperty("javaparameters",
                    "-XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:G1NewSizePercent=20 -XX:G1ReservePercent=20 -XX:MaxGCPauseMillis=50 -XX:G1HeapRegionSize=32M");

            this.maximiseMinecraft = Boolean.parseBoolean(properties.getProperty("maximiseminecraft", "false"));

            this.saveCustomMods = Boolean.parseBoolean(properties.getProperty("savecustommods", "true"));

            this.ignoreJavaOnInstanceLaunch = Boolean
                    .parseBoolean(properties.getProperty("ignorejavaoninstancelaunch", "false"));

            this.advancedBackup = Boolean.parseBoolean(properties.getProperty("advancedbackup", "false"));

            this.sortPacksAlphabetically = Boolean
                    .parseBoolean(properties.getProperty("sortpacksalphabetically", "false"));

            this.keepLauncherOpen = Boolean.parseBoolean(properties.getProperty("keeplauncheropen", "true"));

            this.enableConsole = Boolean.parseBoolean(properties.getProperty("enableconsole", "true"));

            this.enableTrayIcon = Boolean.parseBoolean(properties.getProperty("enabletrayicon", "true"));

            this.enableDiscordIntegration = Boolean
                    .parseBoolean(properties.getProperty("enablediscordintegration", "true"));

            this.enableLeaderboards = Boolean.parseBoolean(properties.getProperty("enableleaderboards", "false"));

            this.enableLogs = Boolean.parseBoolean(properties.getProperty("enablelogs", "true"));

            this.enableServerChecker = Boolean.parseBoolean(properties.getProperty("enableserverchecker", "false"));

            this.enableOpenEyeReporting = Boolean
                    .parseBoolean(properties.getProperty("enableopeneyereporting", "true"));

            this.enableProxy = Boolean.parseBoolean(properties.getProperty("enableproxy", "false"));

            if (this.enableProxy) {
                this.proxyHost = properties.getProperty("proxyhost", null);

                this.proxyPort = Integer.parseInt(properties.getProperty("proxyport", "0"));
                if (this.proxyPort <= 0 || this.proxyPort > 65535) {
                    // Proxy port is invalid so disable proxy
                    LogManager.warn("Tried to set proxy port to " + this.proxyPort + " which is not a valid port! "
                            + "Proxy support disabled!");
                    this.enableProxy = false;
                }

                this.proxyType = properties.getProperty("proxytype", "");
                if (!this.proxyType.equals("SOCKS") && !this.proxyType.equals("HTTP")
                        && !this.proxyType.equals("DIRECT")) {
                    // Proxy type is invalid so disable proxy
                    LogManager.warn("Tried to set proxy type to " + this.proxyType + " which is not valid! Proxy "
                            + "support disabled!");
                    this.enableProxy = false;
                }
            } else {
                this.proxyHost = "";
                this.proxyPort = 0;
                this.proxyType = "";
            }

            this.serverCheckerWait = Integer.parseInt(properties.getProperty("servercheckerwait", "5"));
            if (this.serverCheckerWait < 1 || this.serverCheckerWait > 30) {
                // Server checker wait should be between 1 and 30
                LogManager.warn("Tried to set server checker wait to " + this.serverCheckerWait + " which is not "
                        + "valid! Must be between 1 and 30. Setting back to default of 5!");
                this.serverCheckerWait = 5;
            }

            this.concurrentConnections = Integer.parseInt(properties.getProperty("concurrentconnections", "8"));
            if (this.concurrentConnections < 1) {
                // Concurrent connections should be more than or equal to 1
                LogManager.warn("Tried to set the number of concurrent connections to " + this.concurrentConnections
                        + " which is not valid! Must be 1 or more. Setting back to default of 8!");
                this.concurrentConnections = 8;
            }

            this.daysOfLogsToKeep = Integer.parseInt(properties.getProperty("daysoflogstokeep", "7"));
            if (this.daysOfLogsToKeep < 1 || this.daysOfLogsToKeep > 30) {
                // Days of logs to keep should be 1 or more but less than 30
                LogManager.warn("Tried to set the number of days worth of logs to keep to " + this.daysOfLogsToKeep
                        + " which is not valid! Must be between 1 and 30 inclusive. Setting back to default of 7!");
                this.daysOfLogsToKeep = 7;
            }

            this.theme = properties.getProperty("theme", Constants.LAUNCHER_NAME);

            this.dateFormat = properties.getProperty("dateformat", "dd/M/yyy");
            if (!this.dateFormat.equalsIgnoreCase("dd/M/yyy") && !this.dateFormat.equalsIgnoreCase("M/dd/yyy")
                    && !this.dateFormat.equalsIgnoreCase("yyy/M/dd")) {
                LogManager.warn("Tried to set the date format to " + this.dateFormat + " which is not valid! Setting "
                        + "back to default of dd/M/yyy!");
                this.dateFormat = "dd/M/yyy";
            }

            String lastAccountTemp = properties.getProperty("lastaccount", "");
            if (!lastAccountTemp.isEmpty()) {
                if (isAccountByName(lastAccountTemp)) {
                    this.account = getAccountByName(lastAccountTemp);
                } else {
                    LogManager.warn(
                            "The Account " + lastAccountTemp + " is no longer available. Logging out of " + "Account!");
                    this.account = null; // Account not found
                }
            }

            this.addedPacks = properties.getProperty("addedpacks", "");
            this.autoBackup = Boolean.parseBoolean(properties.getProperty("autobackup", "false"));
            this.notifyBackup = Boolean.parseBoolean(properties.getProperty("notifybackup", "true"));
            this.dropboxFolderLocation = properties.getProperty("dropboxlocation", "");
        } catch (IOException e) {
            LogManager.logStackTrace(e);
        }
        LogManager.debug("Finished loading properties");
    }

    /**
     * Save the properties to file
     */
    public void saveProperties() {
        try {
            properties.setProperty("firsttimerun", "false");
            properties.setProperty("hadpassworddialog", "true");
            properties.setProperty("hideoldjavawarning", this.hideOldJavaWarning + "");
            properties.setProperty("hidejavaletsencryptwarning", this.hideJavaLetsEncryptWarning + "");
            properties.setProperty("hideJava9Warning", this.hideJava9Warning + "");
            properties.setProperty("language", Language.INSTANCE.getCurrent());
            properties.setProperty("forgelogginglevel", this.forgeLoggingLevel);
            properties.setProperty("initialmemory", this.initialMemory + "");
            properties.setProperty("ram", this.maximumMemory + "");
            properties.setProperty("permGen", this.permGen + "");
            properties.setProperty("windowwidth", this.windowWidth + "");
            properties.setProperty("windowheight", this.windowHeight + "");
            properties.setProperty("usingcustomjavapath", (this.usingCustomJavaPath) ? "true" : "false");
            properties.setProperty("javapath", this.javaPath);
            properties.setProperty("javaparameters", this.javaParamaters);
            properties.setProperty("maximiseminecraft", (this.maximiseMinecraft) ? "true" : "false");
            properties.setProperty("savecustommods", (this.saveCustomMods) ? "true" : "false");
            properties.setProperty("ignorejavaoninstancelaunch", (this.ignoreJavaOnInstanceLaunch) ? "true" : "false");
            properties.setProperty("advancedbackup", (this.advancedBackup) ? "true" : "false");
            properties.setProperty("sortpacksalphabetically", (this.sortPacksAlphabetically) ? "true" : "false");
            properties.setProperty("keeplauncheropen", (this.keepLauncherOpen) ? "true" : "false");
            properties.setProperty("enableconsole", (this.enableConsole) ? "true" : "false");
            properties.setProperty("enabletrayicon", (this.enableTrayIcon) ? "true" : "false");
            properties.setProperty("enablediscordintegration", (this.enableDiscordIntegration) ? "true" : "false");
            properties.setProperty("enableleaderboards", (this.enableLeaderboards) ? "true" : "false");
            properties.setProperty("enablelogs", (this.enableLogs) ? "true" : "false");
            properties.setProperty("enablepacktags", (this.enablePackTags) ? "true" : "false");
            properties.setProperty("enableserverchecker", (this.enableServerChecker) ? "true" : "false");
            properties.setProperty("enableopeneyereporting", (this.enableOpenEyeReporting) ? "true" : "false");
            properties.setProperty("enableproxy", (this.enableProxy) ? "true" : "false");
            properties.setProperty("proxyhost", this.proxyHost);
            properties.setProperty("proxyport", this.proxyPort + "");
            properties.setProperty("proxytype", this.proxyType);
            properties.setProperty("servercheckerwait", this.serverCheckerWait + "");
            properties.setProperty("concurrentconnections", this.concurrentConnections + "");
            properties.setProperty("daysoflogstokeep", this.daysOfLogsToKeep + "");
            properties.setProperty("theme", this.theme);
            properties.setProperty("dateformat", this.dateFormat);
            if (account != null) {
                properties.setProperty("lastaccount", account.getUsername());
            } else {
                properties.setProperty("lastaccount", "");
            }
            properties.setProperty("addedpacks", this.addedPacks);
            properties.setProperty("autobackup", this.autoBackup ? "true" : "false");
            properties.setProperty("notifybackup", this.notifyBackup ? "true" : "false");
            properties.setProperty("dropboxlocation", this.dropboxFolderLocation);
            this.properties.store(new FileOutputStream(propertiesFile), Constants.LAUNCHER_NAME + " Settings");
        } catch (IOException e) {
            LogManager.logStackTrace(e);
        }
    }

    public void addAccount(Account account) {
        this.accounts.add(account);
    }

    public void addCheckingServer(MinecraftServer server) {
        this.checkingServers.add(server);
        this.saveCheckingServers();
    }

    public void removeCheckingServer(MinecraftServer server) {
        this.checkingServers.remove(server);
        this.saveCheckingServers();
        this.startCheckingServers();
    }

    /**
     * Switch account currently used and save it
     *
     * @param account Account to switch to
     */
    public void switchAccount(Account account) {
        if (account == null) {
            LogManager.info("Logging out of account");
            this.account = null;
        } else {
            if (account.isReal()) {
                LogManager.info("Changed account to " + account);
                this.account = account;
            } else {
                LogManager.info("Logging out of account");
                this.account = null;
            }
        }
        refreshVanillaPacksPanel();
        refreshFeaturedPacksPanel();
        refreshPacksPanel();
        reloadInstancesPanel();
        reloadAccounts();
        saveProperties();
    }

    /**
     * Loads the languages for use in the Launcher
     */
    private void loadNews() {
        LogManager.debug("Loading news");
        try {
            java.lang.reflect.Type type = new TypeToken<List<News>>() {
            }.getType();
            File fileDir = new File(getJSONDir(), "news.json");
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(fileDir), "UTF-8"));

            this.news = Gsons.DEFAULT.fromJson(in, type);
            in.close();
        } catch (JsonIOException | JsonSyntaxException | IOException e) {
            LogManager.logStackTrace(e);
        }
        LogManager.debug("Finished loading news");
    }

    /**
     * Loads info about the different Minecraft versions
     */
    private void loadMinecraftVersions() {
        LogManager.debug("Loading Minecraft versions");

        this.minecraftVersions = new HashMap<>();

        try {
            java.lang.reflect.Type type = new TypeToken<List<MinecraftVersion>>() {
            }.getType();
            List<MinecraftVersion> list = Gsons.DEFAULT_ALT
                    .fromJson(new FileReader(new File(getJSONDir(), "minecraftversions.json")), type);

            if (list == null) {
                LogManager.error("Error loading Minecraft Versions. List was null. Exiting!");
                System.exit(1); // Cannot recover from this so exit
            }

            for (MinecraftVersion mv : list) {
                this.minecraftVersions.put(mv.getVersion(), mv);
            }
        } catch (JsonSyntaxException | FileNotFoundException | JsonIOException e) {
            LogManager.logStackTrace(e);
        }
        LogManager.debug("Finished loading Minecraft versions");
    }

    /**
     * Loads the Packs for use in the Launcher
     */
    private void loadPacks() {
        LogManager.debug("Loading packs");
        try {
            java.lang.reflect.Type type = new TypeToken<List<Pack>>() {
            }.getType();
            this.packs = Gsons.DEFAULT.fromJson(new FileReader(new File(getJSONDir(), "packs.json")), type);
        } catch (JsonSyntaxException | FileNotFoundException | JsonIOException e) {
            LogManager.logStackTrace(e);
        }
        LogManager.debug("Finished loading packs");
    }

    /**
     * Loads the Testers and Allowed Players for the packs in the Launcher
     */
    private void loadUsers() {
        LogManager.debug("Loading users");
        List<PackUsers> packUsers = null;
        try {
            java.lang.reflect.Type type = new TypeToken<List<PackUsers>>() {
            }.getType();
            packUsers = com.atlauncher.network.Download.build()
                    .setUrl(String.format("%s/launcher/json/users.json", Constants.DOWNLOAD_SERVER)).asType(type);
        } catch (JsonSyntaxException | JsonIOException e) {
            LogManager.logStackTrace(e);
        }
        if (packUsers == null) {
            this.offlineMode = true;
            return;
        }
        for (PackUsers pu : packUsers) {
            pu.addUsers();
        }
        LogManager.debug("Finished loading users");
    }

    /**
     * Loads the user installed Instances
     */
    private void loadInstances() {
        LogManager.debug("Loading instances");
        this.instances = new ArrayList<>(); // Reset the instances list
        this.instancesV2 = new ArrayList<>(); // Reset the instancesv2 list

        for (String folder : this.getInstancesDir().list(Utils.getInstanceFileFilter())) {
            File instanceDir = new File(this.getInstancesDir(), folder);

            Instance instance = null;
            InstanceV2 instanceV2 = null;

            try {
                try (FileReader fileReader = new FileReader(new File(instanceDir, "instance.json"))) {
                    instanceV2 = Gsons.MINECRAFT.fromJson(fileReader, InstanceV2.class);
                    LogManager.debug("Loaded V2 instance from " + instanceDir);
                } catch (JsonIOException | JsonSyntaxException ignored) {
                    try (FileReader fileReader = new FileReader(new File(instanceDir, "instance.json"))) {
                        instance = Gsons.DEFAULT.fromJson(fileReader, Instance.class);
                    } catch (JsonIOException | JsonSyntaxException e) {
                        LogManager.logStackTrace("Failed to load instance in the folder " + instanceDir, e);
                        continue;
                    }
                }
            } catch (Exception e2) {
                LogManager.logStackTrace("Failed to load instance in the folder " + instanceDir, e2);
                continue;
            }

            if (instance == null && instanceV2 == null) {
                LogManager.error("Failed to load instance in the folder " + instanceDir);
                continue;
            }

            if (instance != null) {
                if (!instance.getDisabledModsDirectory().exists()) {
                    instance.getDisabledModsDirectory().mkdir();
                }

                if (isPackByName(instance.getPackName())) {
                    instance.setRealPack(getPackByName(instance.getPackName()));
                }

                this.instances.add(instance);
            }

            if (instanceV2 != null) {
                this.instancesV2.add(instanceV2);
            }
        }

        LogManager.debug("Finished loading instances");
    }

    public void saveInstances() {
        for (Instance instance : this.instances) {
            File instanceFile = new File(instance.getRootDirectory(), "instance.json");
            FileWriter fw = null;
            BufferedWriter bw = null;
            try {
                if (!instanceFile.exists()) {
                    instanceFile.createNewFile();
                }

                fw = new FileWriter(instanceFile);
                bw = new BufferedWriter(fw);
                bw.write(Gsons.DEFAULT.toJson(instance));
            } catch (IOException e) {
                LogManager.logStackTrace(e);
            } finally {
                try {
                    if (bw != null) {
                        bw.close();
                    }
                    if (fw != null) {
                        fw.close();
                    }
                } catch (IOException e) {
                    LogManager.logStackTrace(
                            "Exception while trying to close FileWriter/BufferedWriter for saving instances "
                                    + "json file.",
                            e);
                }
            }
        }
    }

    /**
     * Loads the saved Accounts
     */
    private void loadAccounts() {
        LogManager.debug("Loading accounts");
        if (userDataFile.exists()) {
            FileInputStream in = null;
            ObjectInputStream objIn = null;
            try {
                in = new FileInputStream(userDataFile);
                objIn = new ObjectInputStream(in);
                Object obj;
                while ((obj = objIn.readObject()) != null) {
                    if (obj instanceof Account) {
                        accounts.add((Account) obj);
                    }
                }
            } catch (EOFException e) {
                // Don't log this, it always happens when it gets to the end of the file
            } catch (IOException | ClassNotFoundException e) {
                LogManager.logStackTrace("Exception while trying to read accounts in from file.", e);
            } finally {
                try {
                    if (objIn != null) {
                        objIn.close();
                    }
                    if (in != null) {
                        in.close();
                    }
                } catch (IOException e) {
                    LogManager.logStackTrace(
                            "Exception while trying to close FileInputStream/ObjectInputStream when reading in " + ""
                                    + "accounts.",
                            e);
                }
            }
        }
        LogManager.debug("Finished loading accounts");
    }

    public void saveAccounts() {
        FileOutputStream out = null;
        ObjectOutputStream objOut = null;
        try {
            out = new FileOutputStream(userDataFile);
            objOut = new ObjectOutputStream(out);
            for (Account account : accounts) {
                objOut.writeObject(account);
            }
        } catch (IOException e) {
            LogManager.logStackTrace(e);
        } finally {
            try {
                if (objOut != null) {
                    objOut.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                LogManager.logStackTrace(
                        "Exception while trying to close FileOutputStream/ObjectOutputStream when saving "
                                + "accounts.",
                        e);
            }
        }
    }

    public void removeAccount(Account account) {
        if (this.account == account) {
            switchAccount(null);
        }
        accounts.remove(account);
        saveAccounts();
        reloadAccounts();
    }

    /**
     * Loads the user servers added for checking
     */
    private void loadCheckingServers() {
        LogManager.debug("Loading servers to check");
        this.checkingServers = new ArrayList<>(); // Reset the list
        if (checkingServersFile.exists()) {
            FileReader fileReader = null;
            try {
                fileReader = new FileReader(checkingServersFile);
            } catch (FileNotFoundException e) {
                LogManager.logStackTrace(e);
                return;
            }

            this.checkingServers = Gsons.DEFAULT.fromJson(fileReader, MinecraftServer.LIST_TYPE);

            if (fileReader != null) {
                try {
                    fileReader.close();
                } catch (IOException e) {
                    LogManager
                            .logStackTrace("Exception while trying to close FileReader when loading servers for server "
                                    + "checker" + " tool.", e);
                }
            }
        }
        LogManager.debug("Finished loading servers to check");
    }

    public void saveCheckingServers() {
        FileWriter fw = null;
        BufferedWriter bw = null;
        try {
            if (!checkingServersFile.exists()) {
                checkingServersFile.createNewFile();
            }

            fw = new FileWriter(checkingServersFile);
            bw = new BufferedWriter(fw);
            bw.write(Gsons.DEFAULT.toJson(this.checkingServers));
        } catch (IOException e) {
            LogManager.logStackTrace(e);
        } finally {
            try {
                if (bw != null) {
                    bw.close();
                }
                if (fw != null) {
                    fw.close();
                }
            } catch (IOException e) {
                LogManager.logStackTrace(
                        "Exception while trying to close FileWriter/BufferedWriter when saving servers for "
                                + "server checker tool.",
                        e);
            }
        }
    }

    public List<MinecraftServer> getCheckingServers() {
        return this.checkingServers;
    }

    /**
     * Finds out if this is the first time the Launcher has been run
     *
     * @return true if the Launcher hasn't been run and setup yet, false for
     *         otherwise
     */
    public boolean isFirstTimeRun() {
        return this.firstTimeRun;
    }

    public boolean isMinecraftLaunched() {
        return this.minecraftLaunched;
    }

    public void setMinecraftLaunched(boolean launched) {
        this.minecraftLaunched = launched;
        App.TRAY_MENU.setMinecraftLaunched(launched);
    }

    /**
     * Get the Packs available in the Launcher
     *
     * @return The Packs available in the Launcher
     */
    public List<Pack> getPacks() {
        return this.packs;
    }

    /**
     * Get the Packs available in the Launcher sorted alphabetically
     *
     * @return The Packs available in the Launcher sorted alphabetically
     */
    public List<Pack> getPacksSortedAlphabetically(boolean isFeatured, boolean isVanilla) {
        List<Pack> packs = new LinkedList<>();

        for (Pack pack : this.packs) {
            if (isFeatured) {
                if (!pack.isFeatured()) {
                    continue;
                }
            }

            if (isVanilla) {
                if (pack.getSafeName().startsWith("VanillaMinecraft")) {
                    packs.add(pack);
                }
            } else {
                if (!pack.getSafeName().startsWith("VanillaMinecraft")) {
                    packs.add(pack);
                }
            }
        }

        packs.sort(Comparator.comparing(Pack::getName));
        return packs;
    }

    /**
     * Get the Packs available in the Launcher sorted by position
     *
     * @return The Packs available in the Launcher sorted by position
     */
    public List<Pack> getPacksSortedPositionally(boolean isFeatured, boolean isVanilla) {
        List<Pack> packs = new LinkedList<>();

        for (Pack pack : this.packs) {
            if (isFeatured) {
                if (!pack.isFeatured()) {
                    continue;
                }
            }

            if (isVanilla) {
                if (pack.getSafeName().startsWith("VanillaMinecraft")) {
                    packs.add(pack);
                }
            } else {
                if (!pack.getSafeName().startsWith("VanillaMinecraft")) {
                    packs.add(pack);
                }
            }
        }

        packs.sort(Comparator.comparingInt(Pack::getPosition));
        return packs;
    }

    public void setPackVisbility(Pack pack, boolean collapsed) {
        if (pack != null && account != null && account.isReal()) {
            if (collapsed) {
                // Closed It
                if (!account.getCollapsedPacks().contains(pack.getName())) {
                    account.getCollapsedPacks().add(pack.getName());
                }
            } else {
                // Opened It
                if (account.getCollapsedPacks().contains(pack.getName())) {
                    account.getCollapsedPacks().remove(pack.getName());
                }
            }
            saveAccounts();
            reloadVanillaPacksPanel();
            reloadFeaturedPacksPanel();
            reloadPacksPanel();
        }
    }

    public void setInstanceVisbility(Instance instance, boolean collapsed) {
        if (instance != null && account.isReal()) {
            if (collapsed) {
                // Closed It
                if (!account.getCollapsedInstances().contains(instance.getName())) {
                    account.getCollapsedInstances().add(instance.getName());
                }
            } else {
                // Opened It
                if (account.getCollapsedInstances().contains(instance.getName())) {
                    account.getCollapsedInstances().remove(instance.getName());
                }
            }
            saveAccounts();
            reloadInstancesPanel();
        }
    }

    /**
     * Get the Instances available in the Launcher
     *
     * @return The Instances available in the Launcher
     */
    public List<Instance> getInstances() {
        return this.instances;
    }

    /**
     * Get the Instances available in the Launcher sorted alphabetically
     *
     * @return The Instances available in the Launcher sorted alphabetically
     */
    public ArrayList<Instance> getInstancesSorted() {
        ArrayList<Instance> instances = new ArrayList<>(this.instances);
        instances.sort(Comparator.comparing(Instance::getName));
        return instances;
    }

    public void setInstanceUnplayable(Instance instance) {
        instance.setUnplayable();
        saveInstances();
        reloadInstancesPanel();
    }

    /**
     * Removes an instance from the Launcher
     *
     * @param instance The Instance to remove from the launcher.
     */
    public void removeInstance(Instance instance) {
        if (this.instances.remove(instance)) {
            Utils.delete(instance.getRootDirectory());
            saveInstances();
            reloadInstancesPanel();
        }
    }

    public void removeInstance(InstanceV2 instance) {
        if (this.instancesV2.remove(instance)) {
            FileUtils.deleteDirectory(instance.getRoot());
            reloadInstancesPanel();
        }
    }

    public boolean canViewSemiPublicPackByCode(String packCode) {
        for (String code : this.addedPacks.split(",")) {
            if (Hashing.md5(code).equals(Hashing.HashCode.fromString(packCode))) {
                return true;
            }
        }
        return false;
    }

    public MinecraftVersion getMinecraftVersion(String version) throws InvalidMinecraftVersion {
        if (this.minecraftVersions.containsKey(version)) {
            return this.minecraftVersions.get(version);
        }
        throw new InvalidMinecraftVersion("No Minecraft version found matching " + version);
    }

    public boolean semiPublicPackExistsFromCode(String packCode) {
        for (Pack pack : this.packs) {
            if (pack.isSemiPublic()) {
                if (Hashing.HashCode.fromString(pack.getCode()).equals(Hashing.HashCode.fromString(packCode))) {
                    return true;
                }
            }
        }
        return false;
    }

    public Pack getSemiPublicPackByCode(String packCode) {
        for (Pack pack : this.packs) {
            if (pack.isSemiPublic()) {
                if (Hashing.HashCode.fromString(pack.getCode()).equals(Hashing.HashCode.fromString(packCode))) {
                    return pack;
                }
            }
        }

        return null;
    }

    public boolean addPack(String packCode) {
        for (Pack pack : this.packs) {
            if (pack.isSemiPublic() && !App.settings.canViewSemiPublicPackByCode(packCode)) {
                if (Hashing.HashCode.fromString(pack.getCode()).equals(Hashing.HashCode.fromString(packCode))) {
                    if (pack.isTester()) {
                        return false;
                    }
                    this.addedPacks += packCode + ",";
                    this.saveProperties();
                    this.refreshVanillaPacksPanel();
                    this.refreshFeaturedPacksPanel();
                    this.refreshPacksPanel();
                    return true;
                }
            }
        }
        return false;
    }

    public void removePack(String packCode) {
        for (String code : this.addedPacks.split(",")) {
            if (Hashing.md5(code).equals(Hashing.HashCode.fromString(packCode))) {
                this.addedPacks = this.addedPacks.replace(code + ",", ""); // Remove the string
                this.saveProperties();
                this.refreshVanillaPacksPanel();
                this.refreshFeaturedPacksPanel();
                this.refreshPacksPanel();
            }
        }
    }

    /**
     * Get the Accounts added to the Launcher
     *
     * @return The Accounts added to the Launcher
     */
    public List<Account> getAccounts() {
        return this.accounts;
    }

    /**
     * Get the News for the Launcher
     *
     * @return The News items
     */
    public List<News> getNews() {
        return this.news;
    }

    /**
     * Get the News for the Launcher in HTML for display on the news panel.
     *
     * @return The HTML for displaying on the News Panel
     */
    public String getNewsHTML() {
        String news = "<html>";
        for (News newsItem : App.settings.getNews()) {
            news += newsItem.getHTML();
            if (App.settings.getNews().get(App.settings.getNews().size() - 1) != newsItem) {
                news += "<hr/>";
            }
        }
        news += "</html>";
        return news;
    }

    /**
     * Get the Languages available in the Launcher
     *
     * @return The Languages available in the Launcher
     */
    public List<String> getLanguages() {
        List<String> langs = new LinkedList<>();
        for (File file : this.getLanguagesDir().listFiles((dir, name) -> name.endsWith(".lang"))) {
            langs.add(file.getName().substring(0, file.getName().lastIndexOf(".")));
        }
        return langs;
    }

    /**
     * Determines if offline mode is enabled or not
     *
     * @return true if offline mode is enabled, false otherwise
     */
    public boolean isInOfflineMode() {
        return this.offlineMode;
    }

    public void checkOnlineStatus() {
        this.offlineMode = false;
        String test = com.atlauncher.network.Download.build()
                .setUrl(String.format("%s/ping", Constants.DOWNLOAD_SERVER)).asString();
        if (test != null && test.equalsIgnoreCase("pong")) {
            reloadVanillaPacksPanel();
            reloadFeaturedPacksPanel();
            reloadPacksPanel();
            reloadInstancesPanel();
        } else {
            this.offlineMode = true;
        }
    }

    /**
     * Sets the launcher to offline mode
     */
    public void setOfflineMode() {
        this.offlineMode = true;
    }

    /**
     * Sets the launcher to online mode
     */
    public void setOnlineMode() {
        this.offlineMode = false;
    }

    /**
     * Returns the JFrame reference of the main Launcher
     *
     * @return Main JFrame of the Launcher
     */
    public Window getParent() {
        return this.parent;
    }

    /**
     * Sets the panel used for Instances
     *
     * @param instancesPanel Instances Panel
     */
    public void setInstancesPanel(InstancesTab instancesPanel) {
        this.instancesPanel = instancesPanel;
    }

    /**
     * Reloads the panel used for Instances
     */
    public void reloadInstancesPanel() {
        if (instancesPanel != null) {
            this.instancesPanel.reload(); // Reload the instances panel
        }
    }

    /**
     * Sets the panel used for Vanilla Packs
     *
     * @param vanillaPacksPanel Vanilla Packs Panel
     */
    public void setVanillaPacksPanel(PacksTab vanillaPacksPanel) {
        this.vanillaPacksPanel = vanillaPacksPanel;
    }

    /**
     * Sets the panel used for Featured Packs
     *
     * @param featuredPacksPanel Featured Packs Panel
     */
    public void setFeaturedPacksPanel(PacksTab featuredPacksPanel) {
        this.featuredPacksPanel = featuredPacksPanel;
    }

    /**
     * Sets the panel used for Packs
     *
     * @param packsPanel Packs Panel
     */
    public void setPacksPanel(PacksTab packsPanel) {
        this.packsPanel = packsPanel;
    }

    /**
     * Sets the panel used for News
     *
     * @param newsPanel News Panel
     */
    public void setNewsPanel(NewsTab newsPanel) {
        this.newsPanel = newsPanel;
    }

    /**
     * Reloads the panel used for News
     */
    public void reloadNewsPanel() {
        this.newsPanel.reload(); // Reload the news panel
    }

    /**
     * Reloads the panel used for Vanilla Packs
     */
    public void reloadVanillaPacksPanel() {
        this.vanillaPacksPanel.reload();
    }

    /**
     * Refreshes the panel used for Vanilla Packs
     */
    public void refreshVanillaPacksPanel() {
        this.vanillaPacksPanel.refresh();
    }

    /**
     * Reloads the panel used for Featured Packs
     */
    public void reloadFeaturedPacksPanel() {
        this.featuredPacksPanel.reload();
    }

    /**
     * Refreshes the panel used for Featured Packs
     */
    public void refreshFeaturedPacksPanel() {
        this.featuredPacksPanel.refresh();
    }

    /**
     * Reloads the panel used for Packs
     */
    public void reloadPacksPanel() {
        this.packsPanel.reload(); // Reload the instances panel
    }

    /**
     * Refreshes the panel used for Packs
     */
    public void refreshPacksPanel() {
        this.packsPanel.refresh(); // Refresh the instances panel
    }

    /**
     * Sets the bottom bar
     *
     * @param bottomBar The Bottom Bar
     */
    public void setBottomBar(LauncherBottomBar bottomBar) {
        this.bottomBar = bottomBar;
    }

    /**
     * Reloads the bottom bar accounts combobox
     */
    public void reloadAccounts() {
        if (this.bottomBar == null) {
            return; // Bottom Bar hasn't been made yet, so don't do anything
        }
        this.bottomBar.reloadAccounts(); // Reload the Bottom Bar accounts combobox
    }

    /**
     * Checks to see if there is already an instance with the name provided or not
     *
     * @param name The name of the instance to check for
     * @return True if there is an instance with the same name already
     */
    public boolean isInstance(String name) {
        for (Instance instance : instances) {
            if (instance.getSafeName().equalsIgnoreCase(name.replaceAll("[^A-Za-z0-9]", ""))) {
                return true;
            }
        }
        return this.instancesV2.stream()
                .anyMatch(i -> i.getSafeName().equalsIgnoreCase(name.replaceAll("[^A-Za-z0-9]", "")));
    }

    /**
     * Finds a Pack from the given ID number
     *
     * @param id ID of the Pack to find
     * @return Pack if the pack is found from the ID
     * @throws InvalidPack If ID is not found
     */
    public Pack getPackByID(int id) throws InvalidPack {
        for (Pack pack : packs) {
            if (pack.getID() == id) {
                return pack;
            }
        }
        throw new InvalidPack("No pack exists with ID " + id);
    }

    /**
     * Checks if there is a pack by the given name
     *
     * @param name name of the Pack to find
     * @return True if the pack is found from the name
     */
    public boolean isPackByName(String name) {
        for (Pack pack : packs) {
            if (pack.getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Finds a Pack from the given name
     *
     * @param name name of the Pack to find
     * @return Pack if the pack is found from the name
     */
    public Pack getPackByName(String name) {
        for (Pack pack : packs) {
            if (pack.getName().equalsIgnoreCase(name)) {
                return pack;
            }
        }
        return null;
    }

    /**
     * Finds a Pack from the given safe name
     *
     * @param name name of the Pack to find
     * @return Pack if the pack is found from the safe name
     */
    public Pack getPackBySafeName(String name) {
        for (Pack pack : packs) {
            if (pack.getSafeName().equalsIgnoreCase(name)) {
                return pack;
            }
        }
        return null;
    }

    /**
     * Checks if there is an instance by the given name
     *
     * @param name name of the Instance to find
     * @return True if the instance is found from the name
     */
    public boolean isInstanceByName(String name) {
        for (Instance instance : instances) {
            if (instance.getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if there is an instance by the given name
     *
     * @param name name of the Instance to find
     * @return True if the instance is found from the name
     */
    public boolean isInstanceBySafeName(String name) {
        for (Instance instance : instances) {
            if (instance.getSafeName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Finds a Instance from the given name
     *
     * @param name name of the Instance to find
     * @return Instance if the instance is found from the name
     */
    public Instance getInstanceByName(String name) {
        for (Instance instance : instances) {
            if (instance.getName().equalsIgnoreCase(name)) {
                return instance;
            }
        }
        return null;
    }

    /**
     * Finds a Instance from the given name
     *
     * @param name name of the Instance to find
     * @return Instance if the instance is found from the name
     */
    public Instance getInstanceBySafeName(String name) {
        for (Instance instance : instances) {
            if (instance.getSafeName().equalsIgnoreCase(name)) {
                return instance;
            }
        }
        return null;
    }

    /**
     * Finds an Account from the given username
     *
     * @param username Username of the Account to find
     * @return Account if the Account is found from the username
     */
    public Account getAccountByName(String username) {
        for (Account account : accounts) {
            if (account.getUsername().equalsIgnoreCase(username)) {
                return account;
            }
        }
        return null;
    }

    /**
     * Finds if a language is available
     *
     * @param name The name of the Language
     * @return true if found, false if not
     */
    public boolean isLanguageByName(String name) {
        return this.getLanguages().contains(name.toLowerCase());
    }

    /**
     * Finds if an Account is available
     *
     * @param username The username of the Account
     * @return true if found, false if not
     */
    public boolean isAccountByName(String username) {
        for (Account account : accounts) {
            if (account.getUsername().equalsIgnoreCase(username)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Finds out if the Launcher Console is visible or not
     *
     * @return true if the console is visible, false if it's been hidden
     */
    public boolean isConsoleVisible() {
        return this.console.isVisible();
    }

    /**
     * Gets the Launcher's current Console instance
     *
     * @return The Launcher's Console instance
     */
    public LauncherConsole getConsole() {
        return this.console;
    }

    public void clearConsole() {
        this.console.clearConsole();
    }

    public void addConsoleListener(WindowAdapter wa) {
        this.console.addWindowListener(wa);
    }

    public String getLog() {
        return this.console.getLog();
    }

    public void showKillMinecraft(Process minecraft) {
        this.minecraftProcess = minecraft;
        this.console.showKillMinecraft();
    }

    public void hideKillMinecraft() {
        this.console.hideKillMinecraft();
    }

    public void killMinecraft() {
        if (this.minecraftProcess != null) {
            LogManager.error("Killing Minecraft");

            if (App.settings.enableDiscordIntegration()) {
                DiscordRPC.discordClearPresence();
            }

            this.minecraftProcess.destroy();
            this.minecraftProcess = null;
        } else {
            LogManager.error("Cannot kill Minecraft as there is no instance open!");
        }
    }

    /**
     * Sets the users current active Language
     *
     * @param language The language to set to
     */
    public void setLanguage(String language) {
        try {
            Language.INSTANCE.load(language);
        } catch (IOException ex) {
            LogManager.logStackTrace("Failed to load language", ex);
        }
    }

    /**
     * Gets the users setting for Forge Logging Level
     *
     * @return The users setting for Forge Logging Level
     */
    public String getForgeLoggingLevel() {
        return this.forgeLoggingLevel;
    }

    /**
     * Sets the users setting for Forge Logging Level
     */
    public void setForgeLoggingLevel(String forgeLoggingLevel) {
        this.forgeLoggingLevel = forgeLoggingLevel;
    }

    public int getInitialMemory() {
        return this.initialMemory;
    }

    public void setInitialMemory(int initialMemory) {
        this.initialMemory = initialMemory;
    }

    public int getMaximumMemory() {
        return this.maximumMemory;
    }

    public void setMaximumMemory(int memory) {
        this.maximumMemory = memory;
    }

    public int getPermGen() {
        return this.permGen;
    }

    public void setPermGen(int permGen) {
        this.permGen = permGen;
    }

    public int getWindowWidth() {
        return this.windowWidth;
    }

    public void setWindowWidth(int windowWidth) {
        this.windowWidth = windowWidth;
    }

    public int getWindowHeight() {
        return this.windowHeight;
    }

    public void setWindowHeight(int windowHeight) {
        this.windowHeight = windowHeight;
    }

    public boolean isUsingCustomJavaPath() {
        return this.usingCustomJavaPath;
    }

    public String getJavaPath() {
        return this.javaPath;
    }

    public void setJavaPath(String javaPath) {
        this.usingCustomJavaPath = !javaPath.equalsIgnoreCase(OS.getDefaultJavaPath());
        this.javaPath = javaPath;
    }

    public String getJavaParameters() {
        return this.javaParamaters;
    }

    public void setJavaParameters(String javaParamaters) {
        this.javaParamaters = javaParamaters;
    }

    public Account getAccount() {
        return this.account;
    }

    /**
     * If the user has selected to start Minecraft maximised
     *
     * @return true if yes, false if not
     */
    public boolean startMinecraftMaximised() {
        return this.maximiseMinecraft;
    }

    public void setStartMinecraftMaximised(boolean maximiseMinecraft) {
        this.maximiseMinecraft = maximiseMinecraft;
    }

    public boolean saveCustomMods() {
        return this.saveCustomMods;
    }

    public void setSaveCustomMods(boolean saveCustomMods) {
        this.saveCustomMods = saveCustomMods;
    }

    public boolean ignoreJavaOnInstanceLaunch() {
        return this.ignoreJavaOnInstanceLaunch;
    }

    public void setIgnoreJavaOnInstanceLaunch(boolean ignoreJavaOnInstanceLaunch) {
        this.ignoreJavaOnInstanceLaunch = ignoreJavaOnInstanceLaunch;
    }

    /**
     * If the user has selected to enable advanced backups
     *
     * @return true if yes, false if not
     */
    public boolean isAdvancedBackupsEnabled() {
        return this.advancedBackup;
    }

    public void setAdvancedBackups(boolean advancedBackup) {
        this.advancedBackup = advancedBackup;
    }

    /**
     * If the user has selected to display packs alphabetically or not
     *
     * @return true if yes, false if not
     */
    public boolean sortPacksAlphabetically() {
        return this.sortPacksAlphabetically;
    }

    public void setSortPacksAlphabetically(boolean sortPacksAlphabetically) {
        this.sortPacksAlphabetically = sortPacksAlphabetically;
    }

    /**
     * If the user has selected to show the console always or not
     *
     * @return true if yes, false if not
     */
    public boolean enableConsole() {
        return this.enableConsole;
    }

    /**
     * If the user has selected to keep the launcher open after Minecraft has closed
     *
     * @return true if yes, false if not
     */
    public boolean keepLauncherOpen() {
        return this.keepLauncherOpen;
    }

    /**
     * If the user has selected to enable the tray icon
     *
     * @return true if yes, false if not
     */
    public boolean enableTrayIcon() {
        return this.enableTrayIcon;
    }

    public boolean enableDiscordIntegration() {
        return this.enableDiscordIntegration;
    }

    public void setEnableConsole(boolean enableConsole) {
        this.enableConsole = enableConsole;
    }

    public void setKeepLauncherOpen(boolean keepLauncherOpen) {
        this.keepLauncherOpen = keepLauncherOpen;
    }

    public String getLastSelectedSync() {
        if (this.lastSelectedSync == null) {
            setLastSelectedSync("Dropbox");
        }
        return this.lastSelectedSync;
    }

    public void setLastSelectedSync(String lastSelected) {
        this.lastSelectedSync = lastSelected;
        saveProperties();
    }

    public boolean getNotifyBackup() {
        return this.notifyBackup;
    }

    public void setNotifyBackup(boolean notify) {
        this.notifyBackup = notify;
        saveProperties();
    }

    public String getDropboxLocation() {
        return this.dropboxFolderLocation;
    }

    public void setDropboxLocation(String dropboxLoc) {
        this.dropboxFolderLocation = dropboxLoc;
        saveProperties();
    }

    public boolean getAutoBackup() {
        return this.autoBackup;
    }

    public void setAutoBackup(boolean enableBackup) {
        this.autoBackup = enableBackup;
        saveProperties();
    }

    public void setEnableTrayIcon(boolean enableTrayIcon) {
        this.enableTrayIcon = enableTrayIcon;
    }

    public void setEnableDiscordIntegration(boolean enableDiscordIntegration) {
        this.enableDiscordIntegration = enableDiscordIntegration;
    }

    public boolean enableLeaderboards() {
        return this.enableLeaderboards;
    }

    public void setEnableLeaderboards(boolean enableLeaderboards) {
        this.enableLeaderboards = enableLeaderboards;
    }

    public boolean enableLogs() {
        return this.enableLogs;
    }

    public void setEnableLogs(boolean enableLogs) {
        this.enableLogs = enableLogs;
    }

    public boolean enableServerChecker() {
        return this.enableServerChecker;
    }

    public void setEnableServerChecker(boolean enableServerChecker) {
        this.enableServerChecker = enableServerChecker;
    }

    public boolean enableOpenEyeReporting() {
        return this.enableOpenEyeReporting;
    }

    public void setEnableOpenEyeReporting(boolean enableOpenEyeReporting) {
        this.enableOpenEyeReporting = enableOpenEyeReporting;
    }

    public boolean getEnableProxy() {
        return this.enableProxy;
    }

    public void setEnableProxy(boolean enableProxy) {
        this.enableProxy = enableProxy;
    }

    public String getProxyHost() {
        return this.proxyHost;
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public int getProxyPort() {
        return this.proxyPort;
    }

    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    public String getProxyType() {
        return this.proxyType;
    }

    public void setProxyType(String proxyType) {
        this.proxyType = proxyType;
    }

    public int getServerCheckerWait() {
        return this.serverCheckerWait;
    }

    public void setServerCheckerWait(int serverCheckerWait) {
        this.serverCheckerWait = serverCheckerWait;
    }

    public int getServerCheckerWaitInMilliseconds() {
        return this.serverCheckerWait * 60 * 1000;
    }

    public int getConcurrentConnections() {
        return this.concurrentConnections;
    }

    public void setConcurrentConnections(int concurrentConnections) {
        this.concurrentConnections = concurrentConnections;
    }

    public int getDaysOfLogsToKeep() {
        return this.daysOfLogsToKeep;
    }

    public void setDaysOfLogsToKeep(int daysOfLogsToKeep) {
        this.daysOfLogsToKeep = daysOfLogsToKeep;
    }

    public String getTheme() {
        return this.theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public File getThemeFile() {
        File theme = new File(this.themesDir, this.theme + ".zip");
        if (theme.exists()) {
            return theme;
        } else {
            return null;
        }
    }

    public String getDateFormat() {
        return this.dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
        Timestamper.updateDateFormat();
    }

    public Proxy getProxy() {
        if (!this.enableProxy) {
            return null;
        }
        if (this.proxy == null) {
            Type type;
            switch (this.proxyType) {
            case "HTTP":
                type = Type.HTTP;
                break;
            case "SOCKS":
                type = Type.SOCKS;
                break;
            case "DIRECT":
                type = Type.DIRECT;
                break;
            default:
                // Oh noes, problem!
                LogManager.warn("Tried to set proxy type to " + this.proxyType + " which is not valid! Proxy support "
                        + "disabled!");
                this.enableProxy = false;
                return null;
            }
            this.proxy = new Proxy(type, new InetSocketAddress(this.proxyHost, this.proxyPort));
        }
        return this.proxy;
    }

    public Proxy getProxyForAuth() {
        if (!this.enableProxy) {
            return Proxy.NO_PROXY;
        }
        if (this.proxy == null) {
            Type type;
            switch (this.proxyType) {
            case "HTTP":
                type = Type.HTTP;
                break;
            case "SOCKS":
                type = Type.SOCKS;
                break;
            case "DIRECT":
                type = Type.DIRECT;
                break;
            default:
                // Oh noes, problem!
                LogManager.warn("Tried to set proxy type to " + this.proxyType + " which is not valid! Proxy support "
                        + "disabled!");
                this.enableProxy = false;
                return Proxy.NO_PROXY;
            }
            this.proxy = new Proxy(type, new InetSocketAddress(this.proxyHost, this.proxyPort));
        }
        return this.proxy;
    }

    public String getUserAgent() {
        return this.userAgent + Constants.LAUNCHER_NAME + "/" + Constants.VERSION + " Java/"
                + Java.getLauncherJavaVersion();
    }

    /**
     * @deprecated
     */
    @Deprecated
    public String getLocalizedString(String string) {
        return Language.INSTANCE.localize(string);
    }

    /**
     * @deprecated
     */
    @Deprecated
    public String getLocalizedString(String string, String replace) {
        return Language.INSTANCE.localize(string).replace("%s", replace);
    }

    public void cloneInstance(Instance instance, String clonedName) {
        Instance clonedInstance = (Instance) instance.clone();
        if (clonedInstance == null) {
            LogManager.error("Error Occured While Cloning Instance! Instance Object Couldn't Be Cloned!");
        } else {
            clonedInstance.setName(clonedName);
            clonedInstance.getRootDirectory().mkdir();
            Utils.copyDirectory(instance.getRootDirectory(), clonedInstance.getRootDirectory());
            this.instances.add(clonedInstance);
            this.saveInstances();
            this.reloadInstancesPanel();
        }
    }

    public void cloneInstance(InstanceV2 instance, String clonedName) {
        InstanceV2 clonedInstance = Gsons.MINECRAFT.fromJson(Gsons.MINECRAFT.toJson(instance), InstanceV2.class);

        if (clonedInstance == null) {
            LogManager.error("Error Occured While Cloning Instance! Instance Object Couldn't Be Cloned!");
        } else {
            clonedInstance.launcher.name = clonedName;
            FileUtils.createDirectory(clonedInstance.getRoot());
            Utils.copyDirectory(instance.getRoot().toFile(), clonedInstance.getRoot().toFile());
            clonedInstance.save();
            this.instancesV2.add(clonedInstance);
            this.reloadInstancesPanel();
        }
    }

    public String getPackInstallableCount() {
        int count = 0;
        for (Pack pack : this.getPacks()) {
            if (pack.canInstall()) {
                count++;
            }
        }
        return count + "";
    }
}
