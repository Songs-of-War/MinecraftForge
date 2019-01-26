/*
 * Minecraft Forge
 * Copyright (c) 2016-2018.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package net.minecraftforge.userdev;

import cpw.mods.modlauncher.api.IEnvironment;
import net.minecraftforge.fml.loading.FMLCommonLaunchHandler;
import net.minecraftforge.fml.loading.LibraryFinder;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static net.minecraftforge.fml.loading.LogMarkers.CORE;

public abstract class FMLUserdevLaunchProvider extends FMLCommonLaunchHandler {
    private static final Logger LOGGER = LogManager.getLogger();
    private Path forgeJar;
    private Path mcJars;

    @Override
    public Path getForgePath(final String mcVersion, final String forgeVersion, final String forgeGroup) {
        final URL forgePath = getClass().getClassLoader().getResource("net/minecraftforge/versions/forge/ForgeVersion.class");
        if (forgePath == null) {
            LOGGER.fatal(CORE, "Unable to locate forge on the classpath");
            throw new RuntimeException("Unable to locate forge on the classpath");
        }
        forgeJar = LibraryFinder.findJarPathFor("ForgeVersion.class", "forge", forgePath);
        return forgeJar;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setup(final IEnvironment environment, final Map<String, ?> arguments) {
        final List<String> mavenRoots = new ArrayList<>((List<String>) arguments.get("mavenRoots"));
        final List<String> mods = new ArrayList<>((List<String>) arguments.get("mods"));
        final String forgeVersion = (String) arguments.get("forgeVersion");
        final String mcVersion = (String) arguments.get("mcVersion");
        final String mcpVersion = (String) arguments.get("mcpVersion");
        final String mcpMappings = (String) arguments.get("mcpMappings");
        final String forgeGroup = (String) arguments.get("forgeGroup");
        final String userdevVersion = mcVersion + "-" + forgeVersion + "_mapped_" + mcpMappings;
        int dirs = forgeGroup.split("\\.").length + 2;
        Path fjroot = forgeJar;
        do {
            fjroot = fjroot.getParent();
        } while (dirs-- > 0);
        final String fjpath = fjroot.toString();
        LOGGER.debug(CORE, "Injecting forge as mod {} from maven path {}", userdevVersion, fjpath);
        mavenRoots.add(fjpath);
        mods.add(forgeGroup+":forge:"+userdevVersion);

        try {
            final Enumeration<URL> resources = ClassLoader.getSystemClassLoader().getResources("META-INF/mods.toml");
            final ArrayList<URL> modstoml = Collections.list(resources);
            modstoml.stream().filter(u-> !u.getPath().contains("!"));

        } catch (IOException e) {
            LOGGER.fatal(CORE,"Error trying to find resources", e);
            throw new RuntimeException("wha?", e);
        }

        LOGGER.fatal(CORE, "Got mod coordinates {} from env", System.getenv("MOD_CLASSES"));

        // "a/b/;c/d/;" -> "modid%%c:\fish\pepper;modid%%c:\fish2\pepper2\;modid2%%c:\fishy\bums;modid2%%c:\hmm"
        final Map<String, List<Path>> modClassPaths = Arrays.stream(System.getenv("MOD_CLASSES").split(File.pathSeparator)).
                map(inp -> inp.split("%%", 2)).map(this::buildModPair).
                collect(Collectors.groupingBy(Pair::getLeft, Collectors.mapping(Pair::getRight, Collectors.toList())));

        LOGGER.info(CORE, "Found supplied mod coordinates [{}]", modClassPaths);

        final List<Pair<Path, List<Path>>> explodedTargets = ((Map<String, List<Pair<Path, List<Path>>>>) arguments).computeIfAbsent("explodedTargets", a -> new ArrayList<>());
        modClassPaths.forEach((modlabel,paths) -> explodedTargets.add(Pair.of(paths.get(0), paths.subList(1, paths.size()))));

        // generics are gross yea?
        ((Map)arguments).put("mavenRoots", mavenRoots);
        ((Map)arguments).put("mods", mods);
    }

    private Pair<String, Path> buildModPair(String[] splitString) {
        String modid = splitString.length == 1 ? "defaultmodid" : splitString[0];
        Path path = Paths.get(splitString[splitString.length - 1]);
        return Pair.of(modid, path);
    }

    @Override
    protected void validatePaths(final Path forgePath, final Path[] mcPaths, final String forgeVersion, final String mcVersion, final String mcpVersion) {

    }

    @Override
    public Path[] getMCPaths(final String mcVersion, final String mcpVersion, final String forgeVersion, final String forgeGroup) {
        final URL mcDataPath = getClass().getClassLoader().getResource("assets/minecraft/lang/en_us.json");
        if (mcDataPath == null) {
            LOGGER.fatal(CORE, "Unable to locate minecraft data on the classpath");
            throw new RuntimeException("Unable to locate minecraft data on the classpath");
        }
        mcJars = LibraryFinder.findJarPathFor("en_us.json","mcdata", mcDataPath);
        return new Path[] {mcJars};
    }
}