package com.licoreria.pos.exception;

public class StockInsuficienteException extends ReglaNegocioException {

    private final String producto;
    private final int stockActualUmm;
    private final int cantidadSolicitadaUmm;

    public StockInsuficienteException(String producto, int stockActualUmm, int cantidadSolicitadaUmm) {
        super(
                "STOCK_INSUFICIENTE",
                "Stock insuficiente para '" + producto + "'. Disponible: " + stockActualUmm
                        + " UMM, solicitado: " + cantidadSolicitadaUmm + " UMM."
        );
        this.producto = producto;
        this.stockActualUmm = stockActualUmm;
        this.cantidadSolicitadaUmm = cantidadSolicitadaUmm;
    }

    public String getProducto() {
        return producto;
    }

    public int getStockActualUmm() {
        return stockActualUmm;
    }

    public int getCantidadSolicitadaUmm() {
        return cantidadSolicitadaUmm;
    }
}
