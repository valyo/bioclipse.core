package net.bioclipse.ui.install.discovery;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.internal.p2.discovery.AbstractCatalogSource;
import org.eclipse.equinox.internal.p2.discovery.AbstractDiscoveryStrategy;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogCategory;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.equinox.internal.p2.discovery.model.Icon;
import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.internal.p2.metadata.TranslationSupport;
import org.eclipse.equinox.internal.p2.ui.discovery.repository.RepositorySource;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.operations.RepositoryTracker;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class BasicRepositoryDiscoveryStrategy extends
                AbstractDiscoveryStrategy {
    private static final String IU_PROPERTY_CATEGORY = "org.eclipse.equinox.p2.type.category"; //$NON-NLS-1$

    private static final String PLUGIN_ID = "org.eclipse.equinox.p2.discovery.repository"; //$NON-NLS-1$

    private final List<URI> locations;

    private final Map<IMetadataRepository, RepositorySource> sourceByRepository;

    private final Map<String, CatalogCategory> categoryById;

    private final Map<String, CatalogItem> catalogItemById;

    protected AbstractCatalogSource                          defaultCatalogSource;

    public BasicRepositoryDiscoveryStrategy() {
        this.locations = new ArrayList<URI>();
        this.sourceByRepository = new HashMap<IMetadataRepository, RepositorySource>();
        this.categoryById = new HashMap<String, CatalogCategory>();
        this.catalogItemById = new HashMap<String, CatalogItem>();

        
    }
    
    public void setCategories(List<CatalogCategory> categories) {
        super.setCategories( categories );
     // Add a default category to avoid missing category dialog
        CatalogCategory category = new CatalogCategory();
        category.setId( "net.bioclipse.install.catalog.default" );
        // category.setDescription(getProperty(candidate,
        // IInstallableUnit.PROP_DESCRIPTION));
        category.setName( "Bioclipse" );
        // category.setSource( getSource( repository ) );
        // category.setData( candidate );

        categoryById.put( category.getId(), category );
        categories.add( category );
    }

    /*
     * (non-Javadoc)
     * @see
     * net.bioclipse.ui.install.commands.IRepositoryDiscoveryStrategy#addLocation
     * (java.net.URI)
     */
    public void addLocation( URI location ) {
        locations.add( location );
    }

    /*
     * (non-Javadoc)
     * @see
     * net.bioclipse.ui.install.commands.IRepositoryDiscoveryStrategy#removeLocation
     * (java.net.URI)
     */
    public void removeLocation(URI location) {
        locations.remove(location);
    }

    /* (non-Javadoc)
     * @see net.bioclipse.ui.install.commands.IRepositoryDiscoveryStrategy#performDiscovery(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public void performDiscovery(IProgressMonitor progressMonitor) throws CoreException {
        // ignore
        SubMonitor monitor = SubMonitor.convert(progressMonitor);
        monitor.setWorkRemaining(100);
        try {
            List<IMetadataRepository> repositories = addRepositories(monitor.newChild(50));
            queryInstallableUnits(monitor.newChild(50), repositories);
            connectCategories();
        } catch (ProvisionException e) {
            throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID, "Failed to process repository contents", e)); //$NON-NLS-1$
        }
    }

    @SuppressWarnings("restriction")
    private void connectCategories() {

        Logger logger = LoggerFactory
                        .getLogger( BasicRepositoryDiscoveryStrategy.class );
        logger.debug( categories.size() + " categories found: " );
        for(CatalogCategory c:categories) {
            logger.debug( c.getId() );
        }
        if ( categories.size() == 2 ) {
            CatalogCategory catalog =
                            categoryById.get( "net.bioclipse.install.catalog.default" );
            for( CatalogItem id: catalogItemById.values()) {
                id.setCategoryId( catalog.getId() );
            }
            return;
        }
        for (CatalogCategory category : categories) {
            if (category.getData() instanceof IInstallableUnit) {
                IInstallableUnit categoryIU = (IInstallableUnit) category.getData();
                Collection<IRequirement> required = categoryIU.getRequirements();
                for (IRequirement requirement : required) {
                    if (requirement instanceof IRequiredCapability) {
                        IRequiredCapability capability = (IRequiredCapability) requirement;
                        CatalogItem item = catalogItemById.get(capability.getName());
                        if (item != null) {
                            item.setCategoryId(category.getId());
                        }
                    }
                }
            }
        }
    }

    private List<IMetadataRepository> addRepositories(SubMonitor monitor) throws ProvisionException {
        ProvisioningSession session = ProvisioningUI.getDefaultUI().getSession();

        monitor.setWorkRemaining(locations.size());

        RepositoryTracker repositoryTracker = ProvisioningUI.getDefaultUI().getRepositoryTracker();
        for (URI location : locations) {
            repositoryTracker.addRepository(location, null, session);
            monitor.worked(1);
        }

        // fetch meta-data for these repositories
        ArrayList<IMetadataRepository> repositories = new ArrayList<IMetadataRepository>();
        IMetadataRepositoryManager manager = (IMetadataRepositoryManager) session.getProvisioningAgent().getService(IMetadataRepositoryManager.SERVICE_NAME);
        for (URI uri : locations) {
            IMetadataRepository repository = manager.loadRepository(uri, monitor.newChild(1));
            repositories.add(repository);
        }
        return repositories;
    }

    protected void checkCancelled( IProgressMonitor monitor ) {
        if (monitor.isCanceled()) {
            throw new OperationCanceledException();
        }
    }

    protected void queryInstallableUnits( SubMonitor monitor,
                                          List<IMetadataRepository> repositories ) {
        monitor.setWorkRemaining(repositories.size());
        for (final IMetadataRepository repository : repositories) {
            checkCancelled(monitor);
            IQuery<IInstallableUnit> query = QueryUtil.createMatchQuery(//
                            "id ~= /net.bioclipse.data.*/ " ); //$NON-NLS-1$
            IQueryResult<IInstallableUnit> result = repository.query(query, monitor.newChild(1));
            for (Iterator<IInstallableUnit> iter = result.iterator(); iter.hasNext();) {
                process(repository, iter.next());
            }
        }
    }

    protected void process(IMetadataRepository repository, IInstallableUnit candidate) {
        if (isCategory(candidate)) {
            processCategory(repository, candidate);
        } else {
            processCatalogItem(repository, candidate);
        }
    }

    private CatalogItem processCatalogItem(IMetadataRepository repository, IInstallableUnit candidate) {
        CatalogItem item = catalogItemById.get(candidate.getId());
        if (item != null) {
            return item;
        }

        item = new CatalogItem();
        item.setId(candidate.getId());
        item.setDescription(getProperty(candidate, IInstallableUnit.PROP_DESCRIPTION));
        item.setName(getProperty(candidate, IInstallableUnit.PROP_NAME));
        item.setProvider(getProperty(candidate, IInstallableUnit.PROP_PROVIDER));
        item.setSource(getSource(repository));
        item.setData(candidate);
        item.setSiteUrl(repository.getLocation().toString());
        
        Icon icon = getIcon( candidate.getId() );
        item.setIcon( icon );

        item.getInstallableUnits().add( item.getId() );
        catalogItemById.put(item.getId(), item);
        items.add(item);
        return item;
    }

    protected Icon getIcon( String id ) {
        return null;
    }

    /* (non-Javadoc)
     * @see net.bioclipse.ui.install.commands.IRepositoryDiscoveryStrategy#getProperty(org.eclipse.equinox.p2.metadata.IInstallableUnit, java.lang.String)
     */
    public String getProperty(IInstallableUnit candidate, String key) {
        String value = TranslationSupport.getInstance().getIUProperty(candidate, key);
        return (value != null) ? value : "";
    }

    protected AbstractCatalogSource getSource( IMetadataRepository repository ) {

        RepositorySource source = sourceByRepository.get( repository );
        if ( source == null ) {
            source = new RepositorySource( repository );
            sourceByRepository.put( repository, source );
        }
        return source;
    }

    private CatalogCategory processCategory(IMetadataRepository repository, IInstallableUnit candidate) {
        CatalogCategory category = categoryById.get(candidate.getId());
        if (category != null) {
            return category;
        }

        category = new CatalogCategory();
        category.setId(candidate.getId());
        category.setDescription(getProperty(candidate, IInstallableUnit.PROP_DESCRIPTION));
        category.setName(getProperty(candidate, IInstallableUnit.PROP_NAME));
        category.setSource(getSource(repository));
        category.setData(candidate);

        categoryById.put(category.getId(), category);
        categories.add(category);
        return category;
    }

    private Boolean isCategory(IInstallableUnit candidate) {
        return Boolean.valueOf(candidate.getProperty(IU_PROPERTY_CATEGORY));
    }
}
