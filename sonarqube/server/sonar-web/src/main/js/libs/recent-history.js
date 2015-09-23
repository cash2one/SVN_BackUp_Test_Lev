window.Sonar = {};

window.Sonar.RecentHistory = function () {
};

window.Sonar.RecentHistory.prototype.getRecentHistory = function () {
  var sonarHistory = localStorage.getItem('sonar_recent_history');
  if (sonarHistory == null) {
    sonarHistory = [];
  } else {
    sonarHistory = JSON.parse(sonarHistory);
  }
  return sonarHistory;
};

window.Sonar.RecentHistory.prototype.clear = function () {
  localStorage.removeItem('sonar_recent_history');
};

window.Sonar.RecentHistory.prototype.add = function (resourceKey, resourceName, icon) {
  var sonarHistory = this.getRecentHistory();

  if (resourceKey !== '') {
    var newEntry = {'key': resourceKey, 'name': resourceName, 'icon': icon};
    // removes the element of the array if it exists
    for (var i = 0; i < sonarHistory.length; i++) {
      var item = sonarHistory[i];
      if (item.key === resourceKey) {
        sonarHistory.splice(i, 1);
        break;
      }
    }
    // then add it to the beginning of the array
    sonarHistory.unshift(newEntry);
    // and finally slice the array to keep only 10 elements
    sonarHistory = sonarHistory.slice(0, 10);

    localStorage.setItem('sonar_recent_history', JSON.stringify(sonarHistory));
  }
};

window.Sonar.RecentHistory.prototype.populateRecentHistoryPanel = function () {
  var historyLinksList = jQuery('#recent-history-list');
  historyLinksList.empty();

  var recentHistory = this.getRecentHistory();
  if (recentHistory.length === 0) {
    jQuery('#recent-history').hide();
  } else {
    recentHistory.forEach(function (resource) {
      historyLinksList.append('<li><i class="icon-qualifier-' + resource.icon + '"></i><a href="' +
          baseUrl + '/dashboard/index/' + resource.key + window.dashboardParameters() + '"> ' +
          resource.name + '</a></li>');
    });
    jQuery('#recent-history').show();
  }
};
