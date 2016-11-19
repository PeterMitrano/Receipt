package mitrano.peter.receipt;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class DetailsActivity extends Activity implements View.OnClickListener {

    public static final int REQUEST_RECEIPT_DETAILS = 1000;
    public static final int RESULT_SAVE = 1;
    public static final int RESULT_CANCEL = 1;

    private Button saveButton;
    private Button cancelButton;
    private EditText receiptAmountEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.receipt_details);

        saveButton = (Button) findViewById(R.id.receipt_details_save_button);
        cancelButton = (Button) findViewById(R.id.receipt_detail_cancel_button);
        receiptAmountEdit = (EditText) findViewById(R.id.receipt_amount_edit);

        saveButton.setOnClickListener(this);
        cancelButton.setOnClickListener(this);
        receiptAmountEdit.addTextChangedListener(new CurrencyTextWatcher());
    }

    @Override
    public void onClick(View v) {
        Intent resultsIntent = new Intent();
        switch (v.getId()) {
            case R.id.receipt_detail_cancel_button:
                setResult(RESULT_CANCEL, resultsIntent);
                finish();
                break;
            case R.id.receipt_details_save_button:
                resultsIntent.putExtra(getString(R.string.details_amount),
                        receiptAmountEdit.getText().toString());
                setResult(RESULT_SAVE, resultsIntent);
                finish();
                break;
        }
    }
}
