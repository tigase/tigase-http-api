@import tigase.http.modules.rest.docs.Model
@import java.util.List
@import java.util.stream.Collectors
@param Model model
@if(model != null)
        <table class="table table-sm">
            <thead>
            <tr>
                <th>Name</th>
                <th>Required</th>
                <th>Type</th>
                <th>Available values</th>
            </tr>
            </thead>
            <tbody>
            @for(Model.Field field : model.getFields())
                <tr>
                    <td>${field.getName()}</td>
                    <td>${field.isRequired()}</td>
                    @if(field.getModel() == null)
                        <td>${field.getTypeString()}</td>
                        <td>
                            @if(field.getPossibleValues() != null)
                                @for(String value : field.getPossibleValues())
                                    <div class="w-100">${value}</div>
                                @endfor
                            @endif
                        </td>
                    @else
                        <td colspan="2">
                            @template.model(model = field.getModel())
                        </td>
                    @endif
                </tr>
            @endfor
            </tbody>
        </table>
@endif