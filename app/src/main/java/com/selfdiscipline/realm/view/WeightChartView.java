package com.selfdiscipline.realm.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import com.selfdiscipline.realm.R;
import com.selfdiscipline.realm.model.WeightRecord;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WeightChartView extends View {
    private List<WeightRecord> records = new ArrayList<>();
    private Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint grid = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    public WeightChartView(Context c, AttributeSet a) { super(c,a); init(); }
    private void init(){ line.setStrokeWidth(5); line.setStyle(Paint.Style.STROKE); grid.setStrokeWidth(1); text.setTextSize(28); }
    public void setRecords(List<WeightRecord> r){ records=new ArrayList<>(r); Collections.sort(records,(a,b)->a.date.compareTo(b.date)); invalidate(); }
    @Override protected void onDraw(Canvas c){ super.onDraw(c); line.setColor(getResources().getColor(R.color.color_chart_line)); grid.setColor(getResources().getColor(R.color.color_chart_grid)); text.setColor(getResources().getColor(R.color.color_text_sub)); int w=getWidth(), h=getHeight(); int pad=40; c.drawLine(pad,h-pad,w-pad,h-pad,grid); c.drawLine(pad,pad,pad,h-pad,grid); if(records.size()<2){ c.drawText(getResources().getString(R.string.text_weight_chart_not_enough), pad, h/2, text); return;} float min=999,max=0; for(WeightRecord r:records){ if(r.weight<min)min=r.weight; if(r.weight>max)max=r.weight;} if(max-min<1) max=min+1; float prevX=0, prevY=0; for(int i=0;i<records.size();i++){ float x=pad+(w-2*pad)*(i/(float)(records.size()-1)); float y=(h-pad)-(h-2*pad)*((records.get(i).weight-min)/(max-min)); c.drawCircle(x,y,6,line); if(i>0)c.drawLine(prevX,prevY,x,y,line); prevX=x; prevY=y;} c.drawText(String.format("%.1fkg", max), pad+8, pad+28, text); c.drawText(String.format("%.1fkg", min), pad+8, h-pad-8, text); }
}
