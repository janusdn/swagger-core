package io.swagger.jaxrs.config;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.annotations.Api;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.config.FilterFactory;
import io.swagger.config.Scanner;
import io.swagger.config.ScannerFactory;
import io.swagger.config.SwaggerConfig;
import io.swagger.core.filter.SwaggerSpecFilter;
import io.swagger.jaxrs.Reader;
import io.swagger.models.Contact;
import io.swagger.models.Info;
import io.swagger.models.License;
import io.swagger.models.Scheme;
import io.swagger.models.Swagger;

public class BeanConfig extends AbstractScanner implements Scanner, SwaggerConfig, ReaderConfig {
	Logger LOGGER = LoggerFactory.getLogger(BeanConfig.class);

	Reader reader = new Reader(new Swagger());

	String resourcePackage;
	String[] schemes;
	String title;
	String version;
	String description;
	String termsOfServiceUrl;
	String contact;
	String license;
	String licenseUrl;
	String filterClass;

	Info info;
	String host;
	String basePath;

	boolean isScanAllResources;

	Set<String> ignoredRoutes = new LinkedHashSet<String>();

	public String getResourcePackage() {
		return this.resourcePackage;
	}

	public void setResourcePackage(String resourcePackage) {
		this.resourcePackage = resourcePackage;
	}

	public String[] getSchemes() {
		return this.schemes;
	}

	public void setSchemes(String[] schemes) {
		this.schemes = schemes;
	}

	public String getTitle() {
		return this.title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getVersion() {
		return this.version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getTermsOfServiceUrl() {
		return this.termsOfServiceUrl;
	}

	public void setTermsOfServiceUrl(String termsOfServiceUrl) {
		this.termsOfServiceUrl = termsOfServiceUrl;
	}

	public String getContact() {
		return this.contact;
	}

	public void setContact(String contact) {
		this.contact = contact;
	}

	public String getLicense() {
		return this.license;
	}

	public void setLicense(String license) {
		this.license = license;
	}

	public String getLicenseUrl() {
		return this.licenseUrl;
	}

	public void setLicenseUrl(String licenseUrl) {
		this.licenseUrl = licenseUrl;
	}

	public Info getInfo() {
		return this.info;
	}

	public void setInfo(Info info) {
		this.info = info;
	}

	public String getHost() {
		return this.host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	@Override
	public String getFilterClass() {
		return this.filterClass;
	}

	public void setFilterClass(String filterClass) {
		this.filterClass = filterClass;
	}

	public String getBasePath() {
		return this.basePath;
	}

	public void setBasePath(String basePath) {
		if (!"".equals(basePath) && (basePath != null)) {
			if (!basePath.startsWith("/")) {
				this.basePath = "/" + basePath;
			} else {
				this.basePath = basePath;
			}
		}
	}

	public void setPrettyPrint(String prettyPrint) {
		if (prettyPrint != null) {
			this.prettyPrint = Boolean.parseBoolean(prettyPrint);
		}
	}

	public boolean getScan() {
		return true;
	}

	public void setScan(boolean shouldScan) {
		Set<Class<?>> classes = this.classes();
		if (classes != null) {
			Swagger swagger = this.reader.read(classes);
			if (StringUtils.isNotBlank(this.host)) {
				swagger.setHost(this.host);
			}

			if (StringUtils.isNotBlank(this.basePath)) {
				swagger.setBasePath(this.basePath);
			}

			this.updateInfoFromConfig();
		}
		ScannerFactory.setScanner(this);
	}

	@Override
	public Set<Class<?>> classes() {
		ConfigurationBuilder config = new ConfigurationBuilder();
		Set<String> acceptablePackages = new HashSet<String>();

		boolean allowAllPackages = false;

		if ((this.resourcePackage != null) && !"".equals(this.resourcePackage)) {
			String[] parts = this.resourcePackage.split(",");
			for (String pkg : parts) {
				if (!"".equals(pkg)) {
					acceptablePackages.add(pkg);
					config.addUrls(ClasspathHelper.forPackage(pkg));
				}
			}
		} else {
			allowAllPackages = true;
		}

		config.setScanners(new ResourcesScanner(), new TypeAnnotationsScanner(), new SubTypesScanner());

		final Reflections reflections = new Reflections(config);
		Set<Class<?>> classes = reflections.getTypesAnnotatedWith(Api.class);
		classes.addAll(reflections.getTypesAnnotatedWith(javax.ws.rs.Path.class));
		classes.addAll(reflections.getTypesAnnotatedWith(SwaggerDefinition.class));

		Set<Class<?>> output = new HashSet<Class<?>>();
		for (Class<?> cls : classes) {
			if (allowAllPackages) {
				output.add(cls);
			} else {
				for (String pkg : acceptablePackages) {
					if (cls.getPackage().getName().startsWith(pkg)) {
						output.add(cls);
					}
				}
			}
		}
		return output;
	}

	private void updateInfoFromConfig() {
		this.info = this.getSwagger().getInfo();
		if (this.info == null) {
			this.info = new Info();
		}

		if (StringUtils.isNotBlank(this.description)) {
			this.info.description(this.description);
		}

		if (StringUtils.isNotBlank(this.title)) {
			this.info.title(this.title);
		}

		if (StringUtils.isNotBlank(this.version)) {
			this.info.version(this.version);
		}

		if (StringUtils.isNotBlank(this.termsOfServiceUrl)) {
			this.info.termsOfService(this.termsOfServiceUrl);
		}

		if (this.contact != null) {
			this.info.contact(new Contact()
					.name(this.contact));
		}
		if ((this.license != null) && (this.licenseUrl != null)) {
			this.info.license(new License()
					.name(this.license)
					.url(this.licenseUrl));
		}
		if (this.schemes != null) {
			for (String scheme : this.schemes) {
				this.reader.getSwagger().scheme(Scheme.forValue(scheme));
			}
		}

		this.reader.getSwagger().setInfo(this.info);
	}

	public Swagger getSwagger() {
		return this.reader.getSwagger();
	}

	@Override
	public Swagger configure(Swagger swagger) {
		if (this.schemes != null) {
			for (String scheme : this.schemes) {
				swagger.scheme(Scheme.forValue(scheme));
			}
		}
		if (this.filterClass != null) {
			try {
				SwaggerSpecFilter filter = (SwaggerSpecFilter) Class.forName(this.filterClass).newInstance();
				if (filter != null) {
					FilterFactory.setFilter(filter);
				}
			} catch (Exception e) {
				this.LOGGER.error("failed to load filter", e);
			}
		}
		return swagger.info(this.info)
				.host(this.host)
				.basePath(this.basePath);
	}

	@Override
	public boolean isScanAllResources() {
		return this.isScanAllResources;
	}

	@Override
	public Collection<String> getIgnoredRoutes() {
		return this.ignoredRoutes;
	}

	public void setScanAllResources(boolean isScanAllResources) {
		this.isScanAllResources = isScanAllResources;
	}

	/**
	 * comma separated string, follow the same format as defined in
	 * ServletConfig see ReaderConfigUtils.initReaderConfig
	 * 
	 * @param ignoredRoutes
	 */
	public void setIgnoredRoutes(String ignoredRoutes) {
		if (ignoredRoutes == null) {
			return;
		}

		for (String item : StringUtils.trimToEmpty(ignoredRoutes).split(",")) {
			final String route = StringUtils.trimToNull(item);
			if (route != null) {
				this.ignoredRoutes.add(route);
			}
		}
	}
}
