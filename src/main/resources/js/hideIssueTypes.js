(function () {
    var pattern = "[hide]";
    var discarded = {}
    discarded['DocTest'] = true;
    discarded['ProductOwnerReview'] = true;
    discarded['DesignReview'] = true;
    discarded['CrossTest'] = true;
    discarded['CodeReview'] = true;
    discarded['CompTest'] = true;
    discarded['MergeToRelease'] = true;
    discarded['IntegrTest'] = true;
    discarded['CreateTechDocument'] = true;

    function init(el) {
        jQuery(el).find('option').each(function(k, e) {
            e = jQuery(e)
            var type = e.text().trim();
            if (discarded[type]) {
                e.remove();
            }
        });
    }

    JIRA.bind("selected", function (e, context) {
        if (e.target.id == 'issuetype') {
            init(e.target);
        }

    });

})();
