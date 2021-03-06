/*
 * Minecraft Forge
 * Copyright (c) 2016-2019.
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

package net.minecraftforge.fml.loading.moddiscovery;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import net.minecraftforge.fml.loading.StringUtils;
import net.minecraftforge.forgespi.language.IConfigurable;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.forgespi.language.MavenVersionAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.artifact.versioning.VersionRange;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import static net.minecraftforge.fml.loading.LogMarkers.LOADING;

public class ModFileInfo implements IModFileInfo, IConfigurable
{
    private static final Logger LOGGER = LogManager.getLogger();
    private final IConfigurable config;
    private final ModFile modFile;
    private final URL issueURL;
    private final String modLoader;
    private final VersionRange modLoaderVersion;
    private final boolean showAsResourcePack;
    private final List<IModInfo> mods;
    private final Map<String,Object> properties;

    ModFileInfo(final ModFile modFile, final IConfigurable config)
    {
        this.modFile = modFile;
        this.config = config;
        this.modLoader = config.<String>getConfigElement("modLoader").
                orElseThrow(()->new InvalidModFileException("Missing ModLoader in file", this));
        this.modLoaderVersion = config.<String>getConfigElement("loaderVersion").
                map(MavenVersionAdapter::createFromVersionSpec).
                orElseThrow(()->new InvalidModFileException("Missing ModLoader version in file", this));
        this.showAsResourcePack = config.<Boolean>getConfigElement("showAsResourcePack").orElse(false);
        this.properties = config.<Map<String, Object>>getConfigElement("properties").orElse(Collections.emptyMap());
        this.modFile.setFileProperties(this.properties);
        this.issueURL = config.<String>getConfigElement("issueTrackerURL").map(StringUtils::toURL).orElse(null);
        final List<? extends IConfigurable> modConfigs = config.getConfigList("mods");
        if (modConfigs.isEmpty()) {
            throw new InvalidModFileException("Missing mods list", this);
        }
        this.mods = modConfigs.stream()
                .map(mi-> new ModInfo(this, mi))
                .collect(Collectors.toList());
        LOGGER.debug(LOADING, "Found valid mod file {} with {} mods - versions {}",
                this.modFile::getFileName,
                () -> this.mods.stream().map(IModInfo::getModId).collect(Collectors.joining(",", "{", "}")),
                () -> this.mods.stream().map(IModInfo::getVersion).map(Objects::toString).collect(Collectors.joining(",", "{", "}")));
    }

    @Override
    public List<IModInfo> getMods()
    {
        return mods;
    }

    public ModFile getFile()
    {
        return this.modFile;
    }

    @Override
    public String getModLoader()
    {
        return modLoader;
    }

    @Override
    public VersionRange getModLoaderVersion()
    {
        return modLoaderVersion;
    }

    @Override
    public Map<String, Object> getFileProperties() {
        return this.properties;
    }

    public Optional<Manifest> getManifest() {
        return modFile.getLocator().findManifest(modFile.getFilePath());
    }

    @Override
    public boolean showAsResourcePack() {
        return this.showAsResourcePack;
    }

    @Override
    public <T> Optional<T> getConfigElement(final String... key) {
        return this.config.getConfigElement(key);
    }

    @Override
    public List<? extends IConfigurable> getConfigList(final String... key) {
        return this.config.getConfigList(key);
    }
}
