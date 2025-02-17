(function () {
    define(["TemplateManager"], function (TemplateManager) {
        var SchemaListView = Backbone.View.extend({
            schemas: undefined,
            schemaRepositories: undefined,

            events: {
                "click .btn-schema-new": "newSchema",
                "click #btn-schema-save": "createSchema",
                "click #btn-schema-update": "updateSchema",
                "click #btn-schema-cancel": "closeSchemaForm",
                "click .btn-repository-new": "newRepository",
                "click #btn-repository-save": "createRepository",
                "click #btn-repository-update": "updateRepository",
                "click #btn-repository-cancel": "closeRepositoryForm",
                "click tr.schema": "showSchemaEditForm",
                "click tr.repository": "showRepositoryEditForm",
                "click a.xsd-schema-select": "selectSchema",
                "click #btn-schema-reference-add": "addSchemaReference",
                "click input[name='schema-ref']": "showSchemaReferenceSelect"
            },

            initialize: function () {
                this.getSchemas();
                this.getSchemaRepositories();
            },

            render: function () {
                $(this.el).html(TemplateManager.template('SchemaListView', {repositories: this.schemaRepositories, schemas: this.schemas}));
                return this;
            },

            afterRender: function () {
            },

            getSchemas: function () {
                $.ajax({
                    url: "config/xsd-schema",
                    type: 'GET',
                    dataType: "json",
                    success: _.bind(function (response) {
                        this.schemas = response;
                    }, this),
                    async: false
                });
            },

            getSchemaRepositories: function () {
                $.ajax({
                    url: "config/xsd-schema-repository",
                    type: 'GET',
                    dataType: "json",
                    success: _.bind(function (response) {
                        this.schemaRepositories = response;
                    }, this),
                    async: false
                });
            },

            removeSchema: function (event) {
                var encodedSchemaId = $(event.target).closest($("[id]")).attr('id');
                var id = this.extractId(encodedSchemaId);
                var url = "config/xsd-schema/" + id;

                $.ajax({
                    url: url,
                    type: 'DELETE',
                    success: _.bind(function (response) {
                        this.render();
                    }, this),
                    async: true
                });

            },

            removeSchemaRepository: function (event) {
                var encodedId = $(event.target).closest($("[id]")).attr('id');
                var id = this.extractId(encodedId);
                var url = "config/xsd-schema-repository/" + id;

                $.ajax({
                    url: url,
                    type: 'DELETE',
                    success: _.bind(function (response) {
                        this.render();
                    }, this),
                    async: true
                });

            },

            newSchema: function() {
                $('#schema-edit-dialog').html(TemplateManager.template('SchemaEditView', {schema: undefined}));
                $('#schema-edit-dialog .modal').modal();

            },

            newRepository: function() {
                $('#repository-edit-dialog').html(TemplateManager.template('SchemaRepositoryEditView', {schemaRepository: undefined, schemas: this.schemas}));
                $('#repository-edit-dialog .modal').modal();

            },

            showRepositoryEditForm: function() {
                var encodedSchemaId = $(event.target).closest($("[id]")).attr('id');
                var id = this.extractId(encodedSchemaId);
                var url = "config/xsd-schema/" + id;

                $.ajax({
                    url: url,
                    type: 'GET',
                    dataType: "json",
                    success: _.bind(function (response) {
                        $('#schema-edit-dialog').html(TemplateManager.template('SchemaEditView', {schema: response}));
                        $('#schema-edit-dialog .modal').modal();
                    }, this),
                    async: true
                });
            },

            showSchemaEditForm: function() {
                var encodedSchemaId = $(event.target).closest($("[id]")).attr('id');
                var id = this.extractId(encodedSchemaId);
                var url = "config/xsd-schema-repository/" + id;

                $.ajax({
                    url: url,
                    type: 'GET',
                    dataType: "json",
                    success: _.bind(function (response) {
                        $('#repository-edit-dialog').html(TemplateManager.template('SchemaRepositoryEditView', {schemaRepository: response, schemas: this.schemas}));
                        $('#repository-edit-dialog .modal').modal();
                    }, this),
                    async: true
                });
            },

            createSchema: function() {
                var form = $('#schema-edit-form form');
                this.closeSchemaForm();

                var serializedForm = form.serializeObject();
                var jsonForm = JSON.stringify(serializedForm);
                var url = "config/xsd-schema";
                $.ajax({
                    url: url,
                    type: 'POST',
                    dataType: "json",
                    contentType: "application/json",
                    data: jsonForm,
                    success: _.bind(function (response) {
                        this.render();
                    }, this),
                    async: true
                });
                return false;
            },

            createRepository: function() {
                var form = $('#repository-edit-form form');
                this.closeRepositoryForm();

                var serializedForm = form.serializeObject();
                var url = "config/xsd-schema-repository";
                $.ajax({
                    url: url,
                    type: 'POST',
                    dataType: "json",
                    contentType: "application/json",
                    data: this.getSchemaRepositoryJSON(serializedForm),
                    success: _.bind(function (response) {
                        this.render();
                    }, this),
                    async: true
                });
                return false;
            },

            updateSchema: function() {
                var form = $('#schema-edit-form form');
                this.closeSchemaForm();

                var serializedForm = form.serializeObject();
                var schemaId = serializedForm.id;

                if (serializedForm.id != serializedForm.newId) {
                    serializedForm.id = serializedForm.newId;
                }

                serializedForm = _.omit(serializedForm, "newId");

                var jsonForm = JSON.stringify(serializedForm);
                var url = "config/xsd-schema/" + schemaId;
                $.ajax({
                    url: url,
                    type: 'POST',
                    dataType: "json",
                    contentType: "application/json",
                    data: jsonForm,
                    success: _.bind(function (response) {
                        this.render();
                    }, this),
                    async: true
                });
                return false;
            },

            updateRepository: function() {
                var form = $('#repository-edit-form form');
                this.closeRepositoryForm();

                var serializedForm = form.serializeObject();
                var schemaRepositoryId = serializedForm.id;

                if (serializedForm.id != serializedForm.newId) {
                    serializedForm.id = serializedForm.newId;
                }

                serializedForm = _.omit(serializedForm, "newId");

                var url = "config/xsd-schema-repository/" + schemaRepositoryId;
                $.ajax({
                    url: url,
                    type: 'POST',
                    dataType: "json",
                    contentType: "application/json",
                    data: this.getSchemaRepositoryJSON(serializedForm),
                    success: _.bind(function (response) {
                        this.render();
                    }, this),
                    async: true
                });
                return false;
            },

            getSchemaRepositoryJSON: function(serializedForm) {
                var schemas = [];

                $('ul#xsd-schema-refs').children('li').each(function(index) {
                    schemas.push({ ref: $(this).attr('id') });
                });

                var schemaRepository = { id: serializedForm.id, schemas: { schemas: schemas } };

                return JSON.stringify(schemaRepository);
            },

            closeSchemaForm: function() {
                $('#schema-edit-dialog .modal').modal('hide');
            },

            closeRepositoryForm: function() {
                $('#repository-edit-dialog .modal').modal('hide');
            },

            extractId: function(encodedId) {
                var splitString = encodedId.split('-');
                return splitString[splitString.length-1];
            },

            selectSchema: function(event) {
                $('input[name="schema-ref"]').val(event.currentTarget.innerText);
                $('#xsd-schema-ref-dropdown').removeClass('open');
                return false;
            },

            addSchemaReference: function() {
                var schemaId = $('input[name="schema-ref"]').val();
                if (schemaId.length) {
                    if ($('ul#xsd-schema-refs').children('li#' + schemaId).size() == 0) {
                        $('ul#xsd-schema-refs').append('<li id="' + schemaId + '"><i class="icon-file-text-alt"></i>&nbsp;' + schemaId + '</li>');
                    }

                    $('input[name="schema-ref"]').val('');
                }

                return false;
            },

            showSchemaReferenceSelect: function(event) {
                $('#xsd-schema-ref-dropdown').addClass('open');
                event.stopPropagation();
            }

        });

        return SchemaListView;
    });
}).call(this);
