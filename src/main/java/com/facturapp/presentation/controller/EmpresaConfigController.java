package com.facturapp.presentation.controller;

import com.facturapp.application.EmpresaConfigUseCase;
import com.facturapp.domain.model.EmpresaConfig;
import com.facturapp.presentation.util.AlertUtil;
import com.facturapp.presentation.util.AppContext;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

public class EmpresaConfigController implements Initializable {

    // Lista
    @FXML private ListView<EmpresaConfig> listaEmpresas;

    // Formulario
    @FXML private Label lblFormTitulo;
    @FXML private TextField txtNombreEmpresa;
    @FXML private TextField txtNif;
    @FXML private TextField txtNombreEmisor;
    @FXML private TextField txtCargo;
    @FXML private TextField txtTelefono;
    @FXML private TextField txtEmail;
    @FXML private TextField txtWeb;
    @FXML private TextField txtDireccion;
    @FXML private TextField txtCodigoPostal;
    @FXML private TextField txtCiudad;
    @FXML private TextField txtProvincia;
    @FXML private TextField txtCuentaBancaria;
    @FXML private TextArea  txtNotasPie;
    @FXML private Label lblEstado;
    @FXML private Button btnEliminar;

    private EmpresaConfigUseCase useCase;
    private EmpresaConfig empresaEditando; // null = nueva

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        useCase = AppContext.getInstance().getEmpresaConfigUseCase();
        lblEstado.setVisible(false);

        listaEmpresas.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(EmpresaConfig item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.toString());
            }
        });

        listaEmpresas.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, sel) -> { if (sel != null) cargarEnFormulario(sel); }
        );

        cargarLista();
        prepararNuevaEmpresa();
    }

    private void cargarLista() {
        listaEmpresas.setItems(FXCollections.observableArrayList(useCase.listar()));
    }

    private void cargarEnFormulario(EmpresaConfig c) {
        empresaEditando = c;
        lblFormTitulo.setText("Editar empresa");
        set(txtNombreEmpresa, c.getNombreEmpresa());
        set(txtNif,           c.getNif());
        set(txtNombreEmisor,  c.getNombreEmisor());
        set(txtCargo,         c.getCargo());
        set(txtTelefono,      c.getTelefono());
        set(txtEmail,         c.getEmail());
        set(txtWeb,           c.getWeb());
        set(txtDireccion,     c.getDireccion());
        set(txtCodigoPostal,  c.getCodigoPostal());
        set(txtCiudad,        c.getCiudad());
        set(txtProvincia,     c.getProvincia());
        set(txtCuentaBancaria,c.getCuentaBancaria());
        txtNotasPie.setText(c.getNotasPie() != null ? c.getNotasPie() : "");
        btnEliminar.setDisable(false);
        lblEstado.setVisible(false);
    }

    @FXML
    private void onNuevaEmpresa() {
        listaEmpresas.getSelectionModel().clearSelection();
        prepararNuevaEmpresa();
    }

    private void prepararNuevaEmpresa() {
        empresaEditando = null;
        lblFormTitulo.setText("Nueva empresa");
        limpiarFormulario();
        btnEliminar.setDisable(true);
        lblEstado.setVisible(false);
    }

    @FXML
    private void onGuardar() {
        EmpresaConfig c = (empresaEditando != null) ? empresaEditando : new EmpresaConfig();
        c.setNombreEmpresa(get(txtNombreEmpresa));
        c.setNif(get(txtNif));
        c.setNombreEmisor(get(txtNombreEmisor));
        c.setCargo(get(txtCargo));
        c.setTelefono(get(txtTelefono));
        c.setEmail(get(txtEmail));
        c.setWeb(get(txtWeb));
        c.setDireccion(get(txtDireccion));
        c.setCodigoPostal(get(txtCodigoPostal));
        c.setCiudad(get(txtCiudad));
        c.setProvincia(get(txtProvincia));
        c.setCuentaBancaria(get(txtCuentaBancaria));
        c.setNotasPie(txtNotasPie.getText().isBlank() ? null : txtNotasPie.getText().trim());

        try {
            EmpresaConfig guardada = useCase.guardar(c);
            empresaEditando = guardada;
            btnEliminar.setDisable(false);
            cargarLista();
            // Reselect in list
            listaEmpresas.getItems().stream()
                .filter(e -> e.getId().equals(guardada.getId()))
                .findFirst()
                .ifPresent(e -> listaEmpresas.getSelectionModel().select(e));
            mostrarEstado("✓ Empresa guardada correctamente.", true);
        } catch (Exception e) {
            AlertUtil.mostrarError("Error", "No se pudo guardar: " + e.getMessage());
        }
    }

    @FXML
    private void onEliminar() {
        if (empresaEditando == null) return;
        boolean confirm = AlertUtil.mostrarConfirmacion("Eliminar empresa",
            "¿Eliminar la empresa \"" + empresaEditando + "\"?\nLas facturas emitidas a nombre de esta empresa conservarán sus datos en el PDF.");
        if (confirm) {
            try {
                useCase.eliminar(empresaEditando.getId());
                cargarLista();
                prepararNuevaEmpresa();
            } catch (Exception e) {
                AlertUtil.mostrarError("Error", "No se pudo eliminar: " + e.getMessage());
            }
        }
    }

    private void mostrarEstado(String msg, boolean ok) {
        lblEstado.setText(msg);
        lblEstado.getStyleClass().removeAll("form-error", "form-ok");
        lblEstado.getStyleClass().add(ok ? "form-ok" : "form-error");
        lblEstado.setVisible(true);
    }

    private void limpiarFormulario() {
        txtNombreEmpresa.clear(); txtNif.clear(); txtNombreEmisor.clear();
        txtCargo.clear(); txtTelefono.clear(); txtEmail.clear(); txtWeb.clear();
        txtDireccion.clear(); txtCodigoPostal.clear(); txtCiudad.clear();
        txtProvincia.clear(); txtCuentaBancaria.clear(); txtNotasPie.clear();
    }

    private void set(TextField tf, String val) { tf.setText(val != null ? val : ""); }
    private String get(TextField tf) { String v = tf.getText().trim(); return v.isEmpty() ? null : v; }
}
