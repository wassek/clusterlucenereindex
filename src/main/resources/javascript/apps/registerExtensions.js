window.jahia.i18n.loadNamespaces('clusterlucenereindex');

window.jahia.uiExtender.registry.add('adminRoute', 'clusterlucenereindex', {
    targets: ['administration-server-systemHealth:10'],
    requiredPermission: 'adminUsers',
    label: 'clusterlucenereindex:label',
    isSelectable: true,
    iframeUrl: window.contextJsParameters.contextPath + '/cms/adminframe/default/$lang/settings.clusterlucenereindex.html'
});
