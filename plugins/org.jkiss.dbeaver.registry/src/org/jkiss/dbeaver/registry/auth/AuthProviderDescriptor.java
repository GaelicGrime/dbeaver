/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.registry.auth;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.auth.AuthPropertyDescriptor;
import org.jkiss.dbeaver.model.auth.DBAAuthCredentialsProfile;
import org.jkiss.dbeaver.model.auth.DBAAuthProvider;
import org.jkiss.dbeaver.model.auth.DBAAuthProviderDescriptor;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.model.impl.PropertyDescriptor;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * Auth service descriptor
 */
public class AuthProviderDescriptor extends AbstractDescriptor implements DBAAuthProviderDescriptor {

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.auth.provider"; //$NON-NLS-1$

    private final IConfigurationElement cfg;

    private final ObjectType implType;
    private DBAAuthProvider<?> instance;
    private final DBPImage icon;
    private final Map<String, PropertyDescriptor> configurationParameters = new LinkedHashMap<>();
    private final List<DBAAuthCredentialsProfile> credentialProfiles = new ArrayList<>();
    private final boolean configurable;
    private final String[] requiredFeatures;

    public AuthProviderDescriptor(IConfigurationElement cfg) {
        super(cfg);
        this.cfg = cfg;
        this.implType = new ObjectType(cfg, "class");
        this.icon = iconToImage(cfg.getAttribute("icon"));
        this.configurable = CommonUtils.toBoolean(cfg.getAttribute("configurable"));

        for (IConfigurationElement cfgElement : cfg.getChildren("configuration")) {
            for (IConfigurationElement propGroup : ArrayUtils.safeArray(cfgElement.getChildren(PropertyDescriptor.TAG_PROPERTY_GROUP))) {
                String category = propGroup.getAttribute(PropertyDescriptor.ATTR_LABEL);
                IConfigurationElement[] propElements = propGroup.getChildren(PropertyDescriptor.TAG_PROPERTY);
                for (IConfigurationElement prop : propElements) {
                    PropertyDescriptor propertyDescriptor = new PropertyDescriptor(category, prop);
                    configurationParameters.put(CommonUtils.toString(propertyDescriptor.getId()), propertyDescriptor);
                }
            }
        }
        for (IConfigurationElement credElement : cfg.getChildren("credentials")) {
            credentialProfiles.add(new DBAAuthCredentialsProfile(credElement));
        }

        String rfList = cfg.getAttribute("requiredFeatures");
        if (!CommonUtils.isEmpty(rfList)) {
            requiredFeatures = rfList.split(",");
        } else {
            requiredFeatures = null;
        }
    }

    @NotNull
    public String getId() {
        return cfg.getAttribute("id");
    }

    public String getLabel() {
        return cfg.getAttribute("label");
    }

    public String getDescription() {
        return cfg.getAttribute("description");
    }

    public DBPImage getIcon() {
        return icon;
    }

    public boolean isConfigurable() {
        return configurable;
    }

    public List<PropertyDescriptor> getConfigurationParameters() {
        return new ArrayList<>(configurationParameters.values());
    }

    public List<DBAAuthCredentialsProfile> getCredentialProfiles() {
        return new ArrayList<>(credentialProfiles);
    }

    public List<AuthPropertyDescriptor> getCredentialParameters(Set<String> keySet) {
        if (credentialProfiles.size() > 1) {
            for (DBAAuthCredentialsProfile profile : credentialProfiles) {
                if (profile.getCredentialParameters().size() == keySet.size()) {
                    boolean matches = true;
                    for (String paramName : keySet) {
                        if (profile.getCredentialParameter(paramName) == null) {
                            matches = false;
                            break;
                        }
                    }
                    if (matches) {
                        return profile.getCredentialParameters();
                    }
                }
            }
        }
        return credentialProfiles.get(0).getCredentialParameters();
    }

    @NotNull
    public DBAAuthProvider<?> getInstance() {
        if (instance == null) {
            try {
                instance = implType.createInstance(DBAAuthProvider.class);
            } catch (DBException e) {
                throw new IllegalStateException("Can not instantiate auth provider '" + implType.getImplName() + "'", e);
            }
        }
        return instance;
    }

    public String[] getRequiredFeatures() {
        return requiredFeatures;
    }

    @Override
    public String toString() {
        return getId();
    }

}