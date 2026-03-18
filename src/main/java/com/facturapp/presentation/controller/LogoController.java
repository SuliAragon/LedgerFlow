package com.facturapp.presentation.controller;

import com.facturapp.application.LogoUseCase;
import com.facturapp.domain.model.EmpresaLogo;
import com.facturapp.presentation.util.AlertUtil;
import com.facturapp.presentation.util.AppContext;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * Controlador CRUD de logos de empresa.
 * Permite importar imágenes, renombrarlas y elegir cuál usar en los PDFs.
 */
public class LogoController implements Initializable {

    private static final Logger log = Logger.getLogger(LogoController.class.getName());

    @FXML private TableView<EmpresaLogo> tablaLogos;
    @FXML private TableColumn<EmpresaLogo, String> colNombre;
    @FXML private TableColumn<EmpresaLogo, String> colRuta;
    @FXML private TableColumn<EmpresaLogo, String> colFecha;
    @FXML private TableColumn<EmpresaLogo, String> colEstado;
    @FXML private TableColumn<EmpresaLogo, String> colAcciones;

    // Preview
    @FXML private ImageView imgPreview;
    @FXML private Label lblPreviewNombre;
    @FXML private Label lblActivoInfo;

    // Formulario importar
    @FXML private TextField txtNombreLogo;
    @FXML private TextField txtRutaFichero;
    @FXML private Button btnSeleccionarFichero;

    private LogoUseCase logoUseCase;
    private File ficheroSeleccionado;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        logoUseCase = AppContext.getInstance().getLogoUseCase();
        configurarTabla();
        cargarLogos();
        actualizarInfoActivo();
    }

    private void configurarTabla() {
        colNombre.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNombre()));
        colRuta.setCellValueFactory(d -> {
            String ruta = d.getValue().getRutaArchivo();
            // Mostrar solo el nombre del fichero, no la ruta completa
            return new SimpleStringProperty(ruta != null
                ? new File(ruta).getName() : "");
        });
        colFecha.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getFechaSubida() != null
                ? d.getValue().getFechaSubida().toString() : ""));
        colEstado.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().isActivo() ? "★ Activo" : ""));

        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                getStyleClass().removeAll("badge-success");
                if (!empty && "★ Activo".equals(item)) {
                    getStyleClass().add("badge-success");
                }
            }
        });

        colAcciones.setCellFactory(col -> new TableCell<>() {
            final Button btnUsar     = new Button("Usar en PDF");
            final Button btnPreview  = new Button("Ver");
            final Button btnEliminar = new Button("Eliminar");
            final HBox botones = new HBox(6, btnUsar, btnPreview, btnEliminar);

            {
                btnUsar.getStyleClass().add("btn-primary");
                btnPreview.getStyleClass().add("btn-secondary");
                btnEliminar.getStyleClass().add("btn-danger");
                btnUsar.setOnAction(e -> usarLogo(getTableView().getItems().get(getIndex())));
                btnPreview.setOnAction(e -> previsualizarLogo(getTableView().getItems().get(getIndex())));
                btnEliminar.setOnAction(e -> eliminarLogo(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : botones);
            }
        });

        tablaLogos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        // Preview al seleccionar fila
        tablaLogos.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, nuevo) -> { if (nuevo != null) previsualizarLogo(nuevo); }
        );
    }

    private void cargarLogos() {
        tablaLogos.setItems(FXCollections.observableArrayList(logoUseCase.listarLogos()));
    }

    private void actualizarInfoActivo() {
        logoUseCase.obtenerLogoActivo().ifPresentOrElse(
            l -> lblActivoInfo.setText("Logo activo en PDFs: " + l.getNombre()),
            () -> lblActivoInfo.setText("Sin logo activo — los PDFs usarán el texto 'FacturApp'")
        );
    }

    @FXML
    private void onSeleccionarFichero() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Seleccionar imagen de logo");
        chooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"),
            new FileChooser.ExtensionFilter("PNG", "*.png"),
            new FileChooser.ExtensionFilter("JPEG", "*.jpg", "*.jpeg"),
            new FileChooser.ExtensionFilter("Todos los ficheros", "*.*")
        );

        File fichero = chooser.showOpenDialog(btnSeleccionarFichero.getScene().getWindow());
        if (fichero != null) {
            ficheroSeleccionado = fichero;
            txtRutaFichero.setText(fichero.getAbsolutePath());

            // Sugerir nombre si el campo está vacío
            if (txtNombreLogo.getText().isBlank()) {
                String nombreSugerido = fichero.getName()
                    .replaceAll("\\.[^.]+$", "")  // quitar extensión
                    .replace("_", " ").replace("-", " ");
                txtNombreLogo.setText(nombreSugerido);
            }

            // Mostrar preview inmediato
            try {
                Image img = new Image(fichero.toURI().toString(), 200, 200, true, true);
                imgPreview.setImage(img);
                lblPreviewNombre.setText(fichero.getName());
            } catch (Exception ignored) {}
        }
    }

    @FXML
    private void onImportarLogo() {
        String nombre = txtNombreLogo.getText().trim();

        if (nombre.isBlank()) {
            AlertUtil.mostrarAdvertencia("Nombre requerido", "Introduce un nombre para identificar el logo.");
            txtNombreLogo.requestFocus();
            return;
        }
        if (ficheroSeleccionado == null) {
            AlertUtil.mostrarAdvertencia("Sin fichero", "Selecciona un fichero de imagen primero.");
            return;
        }

        try {
            EmpresaLogo nuevo = logoUseCase.importarLogo(nombre, ficheroSeleccionado);
            AlertUtil.mostrarExito("Logo importado",
                "\"" + nuevo.getNombre() + "\" guardado correctamente.\n" +
                "Pulsa 'Usar en PDF' para activarlo en las facturas.");

            txtNombreLogo.clear();
            txtRutaFichero.clear();
            ficheroSeleccionado = null;
            cargarLogos();
            actualizarInfoActivo();

        } catch (Exception e) {
            log.severe("Error importando logo: " + e.getMessage());
            AlertUtil.mostrarError("Error", "No se pudo importar el logo: " + e.getMessage());
        }
    }

    private void usarLogo(EmpresaLogo logo) {
        try {
            logoUseCase.activarLogo(logo.getId());
            cargarLogos();
            actualizarInfoActivo();
            AlertUtil.mostrarExito("Logo activado",
                "\"" + logo.getNombre() + "\" se usará en todas las facturas PDF.");
        } catch (Exception e) {
            AlertUtil.mostrarError("Error", e.getMessage());
        }
    }

    private void previsualizarLogo(EmpresaLogo logo) {
        try {
            File f = new File(logo.getRutaArchivo());
            if (f.exists()) {
                Image img = new Image(f.toURI().toString(), 200, 200, true, true);
                imgPreview.setImage(img);
                lblPreviewNombre.setText(logo.getNombre() + (logo.isActivo() ? " ★" : ""));
            } else {
                imgPreview.setImage(null);
                lblPreviewNombre.setText("Fichero no encontrado");
            }
        } catch (Exception e) {
            imgPreview.setImage(null);
        }
    }

    private void eliminarLogo(EmpresaLogo logo) {
        boolean confirm = AlertUtil.mostrarConfirmacion("Eliminar logo",
            "¿Eliminar el logo \"" + logo.getNombre() + "\"?\n" +
            "Se borrará el registro y el fichero de imagen.");
        if (confirm) {
            try {
                logoUseCase.eliminarLogo(logo.getId());
                imgPreview.setImage(null);
                lblPreviewNombre.setText("Sin selección");
                cargarLogos();
                actualizarInfoActivo();
            } catch (Exception e) {
                AlertUtil.mostrarError("Error", e.getMessage());
            }
        }
    }
}
